package working_project.model;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import working_project.core.Camera;
import working_project.io.FileDialogHandler;
import working_project.marching_cubes.MarchingCubes;
import org.joml.Vector3f;
import working_project.model.ModelLoader.Chunk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import working_project.rendering.Point3D;
import working_project.rendering.Renderer;
import working_project.rendering.Triangle;

public class ModelManager {
    private final ModelLoader loader;
    private final Camera camera;
    private final Renderer renderer;
    private Model model;
    private final float[] modelYaw = {0.0f};
    private final float[] modelPitch = {0.0f};
    private final Vector3f objectCenter = new Vector3f(0, 0, 0);
    private volatile boolean isExporting = false;
    private volatile float exportProgress = 0.0f;
    private volatile String exportStatus = "";
    private final float[] objectColor = {0.0f, 0.5f, 1.0f};

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    public ModelManager(ModelLoader loader, Camera camera, Renderer renderer) {
        this.loader = loader;
        this.camera = camera;
        this.renderer = renderer;
    }

    public void loadModel(
            File file,
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isModelLoaded,
            boolean[] isPointCloud,
            boolean[] onlyPointMode) {
        if (file == null) return;

        String filePath = file.getAbsolutePath();
        String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        if (!"stl".equals(fileExtension) && !"ply".equals(fileExtension) && !"obj".equals(fileExtension)) {
            System.out.println("Ошибка: неподдерживаемый формат файла. Выберите .stl, .ply или .obj.");
            return;
        }

        System.out.println("Загрузка модели из: " + filePath);
        try {
            model = ModelConverter.loadModel(filePath);
            for (ModelLoader.Chunk chunk : chunks) {
                chunk.cleanup();
            }
            chunks.clear();
            points.clear();
            isPointCloud[0] = false;

            if (!model.triangles.isEmpty() && !onlyPointMode[0]) {
                model.computeNormals();
                chunks.addAll(loader.createChunksFromData(model.toChunkData()));
                System.out.println("Загружено " + chunks.size() + " чанков.");
                renderer.cleanupPointsBuffers();
            } else {
                points.addAll(model.vertices);
                isPointCloud[0] = true;
                renderer.initOrUpdatePointsBuffers(points);
                System.out.println("Загружено облако точек с " + points.size() + " точками.");
            }

            isModelLoaded[0] = !chunks.isEmpty() || !points.isEmpty();
            if (isModelLoaded[0]) {
                updateCameraAndCenter(chunks, points);
                renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем начальный цвет
            } else {
                System.out.println("Не удалось загрузить модель: нет чанков или точек.");
                renderer.cleanupPointsBuffers();
            }
        } catch (IOException e) {
            isModelLoaded[0] = false;
            System.err.println("Ошибка загрузки модели: " + e.getMessage());
            e.printStackTrace();
            renderer.cleanupPointsBuffers();
        }
    }

    public void exportModel(FileDialogHandler fileDialogHandler) {
        if (!isModelLoaded() || model == null || model.vertices.isEmpty()) {
            exportStatus = "Невозможно экспортировать: модель не загружена.";
            System.out.println("Невозможно экспортировать: модель не загружена или пуста.");
            return;
        }

        isExporting = true;
        exportStatus = "Starting export...";
        File file = fileDialogHandler.saveFileDialog();
        if (file != null) {
            String filePath = file.getAbsolutePath();
            String extension = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase() : "obj";
            if (!extension.equals("obj") && !extension.equals("stl") && !extension.equals("ply")) {
                filePath += ".obj";
                extension = "obj";
            }
            final String finalFilePath = filePath;
            final String finalExtension = extension;
            executorService.submit(() -> {
                try {
                    exportProgress = 0.0f;
                    long startTime = System.nanoTime();
                    ModelExporter.exportModel(model, new File(finalFilePath), finalExtension);
                    exportProgress = 1.0f;
                    long duration = (System.nanoTime() - startTime) / 1_000_000;
                    exportStatus = "Экспорт завершён за " + duration + " мс: " + finalFilePath;
                    System.out.println("Модель экспортирована в: " + finalFilePath);
                } catch (Exception e) {
                    exportStatus = "Ошибка экспорта: " + e.getMessage();
                    System.err.println("Ошибка экспорта модели: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    isExporting = false;
                }
            });
        } else {
            isExporting = false;
            exportStatus = "Экспорт отменён.";
            System.out.println("Экспорт отменён пользователем.");
        }
    }

    public void applyMarchingCubes(
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isPointCloud,
            boolean[] isRendering,
            float voxelSize,
            float isoLevel) {
        if (!isModelLoaded() || !isPointCloud[0] || points.isEmpty()) {
            System.out.println("Марширующие кубы не могут быть применены: облако точек не загружено.");
            return;
        }

        try {
            MarchingCubes.Mesh marchingMesh = MarchingCubes.processPointCloud(points, voxelSize, isoLevel);
            System.out.println("Марширующие кубы сгенерировали меш с " + marchingMesh.vertices.size() +
                    " вершинами и " + marchingMesh.faces.size() + " гранями.");

            Model newModel = new Model();
            for (double[] vertex : marchingMesh.vertices) {
                newModel.vertices.add(new Point3D((float) vertex[0], (float) vertex[1], (float) vertex[2]));
            }
            for (int[] face : marchingMesh.faces) {
                Point3D p1 = newModel.vertices.get(face[0]);
                Point3D p2 = newModel.vertices.get(face[1]);
                Point3D p3 = newModel.vertices.get(face[2]);
                newModel.triangles.add(new Triangle(p1, p2, p3));
            }
            newModel.computeNormals();

            for (ModelLoader.Chunk chunk : chunks) {
                chunk.cleanup();
            }
            chunks.clear();
            points.clear();
            model = newModel;

            chunks.addAll(loader.createChunksFromData(model.toChunkData()));
            isPointCloud[0] = false;
            isRendering[0] = true;
            System.out.println("Создано " + chunks.size() + " чанков для рендеринга.");
            renderer.cleanupPointsBuffers();

            updateCameraAndCenter(chunks, points);
            renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем текущий цвет
        } catch (Exception e) {
            System.err.println("Ошибка обработки марширующих кубов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeNoise(
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isPointCloud,
            boolean[] isModelLoaded) {
        if (!isModelLoaded() || model == null) return;

        System.out.println("Перед удалением шума: вершин=" + model.vertices.size() + ", треугольников=" + model.triangles.size());
        model.removeNoise(100);
        System.out.println("После удаления шума: вершин=" + model.vertices.size() + ", треугольников=" + model.triangles.size());
        if (model.triangles.isEmpty()) {
            System.err.println("Ошибка: модель пустая после удаления шума.");
            isModelLoaded[0] = false;
            chunks.clear();
        } else {
            for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
            chunks.clear();
            chunks.addAll(loader.createChunksFromData(model.toChunkData()));
            System.out.println("Шум устранён. Чанков: " + chunks.size());
            renderer.cleanupPointsBuffers();

        }
        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем
    }

    public void findLargestComponent(
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isPointCloud) {
        if (!isModelLoaded() || model == null) return;

        if (isPointCloud[0]) {
            System.out.println("Функция не применима к облаку точек.");
        } else {
            System.out.println("Перед нахождением наибольшего объекта: вершин=" + model.vertices.size() + ", треугольников=" + model.triangles.size());
            model.findLargestConnectedComponent();
            System.out.println("После нахождением наибольшего объекта: вершин=" + model.vertices.size() + ", треугольников=" + model.triangles.size());
            for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
            chunks.clear();
            chunks.addAll(loader.createChunksFromData(model.toChunkData()));
            System.out.println("На сцене оставлен главный объект. Чанков: " + chunks.size());
        }
        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем текущий цвет
    }

    public void smoothModel(List<ModelLoader.Chunk> chunks, List<Point3D> points, boolean[] isPointCloud) {
        if (!isModelLoaded() || model == null) return;
        
        System.out.println("Перед сглаживанием: вершин=" + model.vertices.size() + ", треугольников=" + model.triangles.size());
        model.gaussianSmooth(0.5f, 5);
        System.out.println("После сглаживания: вершин=" + model.vertices.size() + ", треугольников=" + model.triangles.size());
        for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
        chunks.clear();
        chunks.addAll(loader.createChunksFromData(model.toChunkData()));
        System.out.println("Модель сглажена. Чанков: " + chunks.size());
        renderer.cleanupPointsBuffers();

        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем текущий цвет
    }

    public void smoothBoundaries(
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isPointCloud,
            boolean[] isModelLoaded) {
        if (!isModelLoaded() || model == null) {
            System.out.println("Cannot smooth boundaries: model not loaded.");
            return;
        }

        if (isPointCloud[0]) {
            System.out.println("Boundary smoothing not applicable to point clouds.");
            return;
        }

        System.out.println("Before boundary smoothing: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
        model.smoothBoundaries(0.6f, 5); // Умеренные параметры: фактор 0.5, 3 итерации
        System.out.println("After boundary smoothing: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());

        if (model.triangles.isEmpty()) {
            System.err.println("Error: Model empty after boundary smoothing.");
            isModelLoaded[0] = false;
            chunks.clear();
        } else {
            for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
            chunks.clear();
            chunks.addAll(loader.createChunksFromData(model.toChunkData()));
            System.out.println("Boundary smoothing applied. Chunks: " + chunks.size());
        }

        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Сохраняем цвет
        updateCameraAndCenter(chunks, points); // Обновляем камеру
    }


    private void updateCameraAndCenter(List<ModelLoader.Chunk> chunks, List<Point3D> points) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        if (!points.isEmpty()) {
            for (Point3D point : points) {
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
                minZ = Math.min(minZ, point.z);
                maxZ = Math.max(maxZ, point.z);
            }
        } else {
            ModelLoader.AABB globalAABB = loader.getGlobalAABB(chunks);
            Vector3f center = globalAABB.center();
            minX = center.x - globalAABB.extent();
            maxX = center.x + globalAABB.extent();
            minY = center.y;
            maxY = center.y + globalAABB.extent();
            minZ = center.z - globalAABB.extent();
            maxZ = center.z + globalAABB.extent();
        }

        objectCenter.set((minX + maxX) / 2.0f, (minY + maxY) / 2.0f, (minZ + maxZ) / 2.0f);
        float extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) / 2.0f;
        camera.setPosition(new Vector3f(objectCenter.x, objectCenter.y, objectCenter.z + extent * 1.5f));
        camera.resetRotation();
        renderer.updateGridSize(extent);
        System.out.println("Модель загружена: камера центрирована на: " + camera.getPosition() + ", размер: " + extent);
    }

    public void selectTriangles(List<Vector2f> lassoPoints, List<Chunk> chunks, float aspectRatio) {
        if (lassoPoints.size() < 3 || chunks.isEmpty()) {
            System.out.println("Cannot select triangles: insufficient lasso points or empty chunks");
            return;
        }

        Matrix4f view = camera.getViewMatrix();
        Matrix4f projection = camera.getProjectionMatrix(aspectRatio, 0.1f, 1000.0f);
        Matrix4f model = new Matrix4f().identity();
        ModelLoader.AABB globalAABB = loader.getGlobalAABB(chunks);
        Vector3f center = globalAABB.center();
        model.translate(center.x, center.y, center.z);
        model.rotate((float) Math.toRadians(modelYaw[0]), 0, 1, 0);
        model.rotate((float) Math.toRadians(modelPitch[0]), 1, 0, 0);
        model.translate(-center.x, -center.y, -center.z);

        List<Vector2f> normalizedLassoPoints = new ArrayList<>();
        float windowWidth = 1920.0f; // TODO: Получать из WindowManager
        float windowHeight = 1080.0f;
        for (Vector2f point : lassoPoints) {
            float x = (point.x / windowWidth) * 2.0f - 1.0f;
            float y = -((point.y / windowHeight) * 2.0f - 1.0f);
            normalizedLassoPoints.add(new Vector2f(x, y));
            System.out.println("Normalized lasso point: (" + x + ", " + y + ")");
        }

        for (Chunk chunk : chunks) {
            List<ModelLoader.Triangle> selectedTriangles = new ArrayList<>();
            int triangleIndex = 0;
            for (ModelLoader.Triangle tri : chunk.triangles) {
                // Центр треугольника
                Vector3f centroid = new Vector3f(tri.vertices[0].position)
                        .add(tri.vertices[1].position)
                        .add(tri.vertices[2].position)
                        .div(3.0f);

                Vector4f clipPos = new Vector4f(centroid, 1.0f);
                clipPos = model.transform(clipPos);
                clipPos = view.transform(clipPos);
                clipPos = projection.transform(clipPos);

                if (clipPos.w <= 0) {
                    System.out.println("Triangle " + triangleIndex + " skipped: behind camera (w=" + clipPos.w + ")");
                    triangleIndex++;
                    continue;
                }

                float ndcX = clipPos.x / clipPos.w;
                float ndcY = clipPos.y / clipPos.w;

                System.out.println("Triangle " + triangleIndex + " centroid NDC: (" + ndcX + ", " + ndcY + ")");

                // Проверяем, находится ли центр внутри лассо
                if (isPointInsidePolygon(new Vector2f(ndcX, ndcY), normalizedLassoPoints)) {
                    selectedTriangles.add(tri);
                    System.out.println("Triangle " + triangleIndex + " selected");
                }
                triangleIndex++;
            }

            chunk.setSelectedTriangles(selectedTriangles);
            System.out.println("Chunk updated with " + selectedTriangles.size() + " selected triangles");
        }

        System.out.println("Selected triangles updated in chunks");
    }

    // лассо: Проверяет, находится ли точка внутри полигона
    private boolean isPointInsidePolygon(Vector2f point, List<Vector2f> polygon) {
        int i, j;
        boolean inside = false;
        for (i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            if (((polygon.get(i).y > point.y) != (polygon.get(j).y > point.y)) &&
                    (point.x < (polygon.get(j).x - polygon.get(i).x) * (point.y - polygon.get(i).y) /
                            (polygon.get(j).y - polygon.get(i).y) + polygon.get(i).x)) {
                inside = !inside;
            }
        }
        return inside;
    }


    public void toggleCameraProjection() {
        camera.toggleProjection();
    }

    public float getModelYaw() {
        return modelYaw[0];
    }

    public float getModelPitch() {
        return modelPitch[0];
    }

    public Vector3f getObjectCenter() {
        return objectCenter;
    }

    public void updateModelYaw(float delta) {
        modelYaw[0] += delta;
    }

    public void updateModelPitch(float delta) {
        modelPitch[0] += delta;
    }

    public void clampModelPitch() {
        modelPitch[0] = Math.max(-89.0f, Math.min(89.0f, modelPitch[0]));
    }

    public boolean isExporting() {
        return isExporting;
    }

    public String getExportStatus() {
        return exportStatus;
    }

    private boolean isModelLoaded() {
        return model != null && (!model.vertices.isEmpty() || !model.triangles.isEmpty());
    }

    public float[] getObjectColor() {
        return objectColor;
    }

    public void setObjectColor(float r, float g, float b) {
        objectColor[0] = r;
        objectColor[1] = g;
        objectColor[2] = b;
        renderer.setObjectColor(r, g, b);
        System.out.println("Цвет модели изменён на: R=" + r + ", G=" + g + ", B=" + b);
    }

    public void cleanup() {
        renderer.cleanup();
    }

}