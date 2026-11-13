package dev.aether.render;

import dev.aether.math.Vec3;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.assimp.Assimp.*;

/** CPU-only OBJ import. Assimp's temporary scene is always released before returning. */
public final class ObjModelLoader {
    private ObjModelLoader() { }

    public static ModelMesh load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj"))
            throw new IllegalArgumentException("Only OBJ models are supported");
        if (!Files.isRegularFile(file)) throw new IOException("Model does not exist: " + file);
        AIScene scene = aiImportFile(file.toAbsolutePath().toString(),
                aiProcess_Triangulate | aiProcess_ValidateDataStructure);
        if (scene == null) throw new IOException("Cannot import " + file.getFileName() + ": " + aiGetErrorString());
        try {
            var parts = new ArrayList<ModelMesh.Part>();
            for (int m = 0; m < scene.mNumMeshes(); m++) {
                AIMesh mesh = AIMesh.create(scene.mMeshes().get(m));
                int triangleCount = 0;
                for (int f = 0; f < mesh.mNumFaces(); f++)
                    if (mesh.mFaces().get(f).mNumIndices() == 3) triangleCount++;
                if (triangleCount == 0) continue;
                float[] vertices = new float[Math.multiplyExact(triangleCount, 24)];
                int cursor = 0;
                var uv = mesh.mTextureCoords(0);
                var normals = mesh.mNormals();
                for (int f = 0; f < mesh.mNumFaces(); f++) {
                    AIFace face = mesh.mFaces().get(f);
                    if (face.mNumIndices() != 3) continue;
                    for (int j = 0; j < 3; j++) {
                        int index = face.mIndices().get(j);
                        var p = mesh.mVertices().get(index);
                        vertices[cursor++] = p.x(); vertices[cursor++] = p.y(); vertices[cursor++] = p.z();
                        vertices[cursor++] = uv == null ? 0 : uv.get(index).x();
                        vertices[cursor++] = uv == null ? 0 : uv.get(index).y();
                        vertices[cursor++] = normals == null ? 0 : normals.get(index).x();
                        vertices[cursor++] = normals == null ? 0 : normals.get(index).y();
                        vertices[cursor++] = normals == null ? 0 : normals.get(index).z();
                    }
                }
                generateMissingNormals(vertices);
                Vec3 color = new Vec3(.8, .8, .8);
                try (var stack = MemoryStack.stackPush()) {
                    var diffuse = AIColor4D.malloc(stack);
                    var material = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));
                    if (aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, 0, 0, diffuse) == aiReturn_SUCCESS)
                        color = new Vec3(diffuse.r(), diffuse.g(), diffuse.b());
                }
                parts.add(new ModelMesh.Part(vertices, color));
            }
            if (parts.isEmpty()) throw new IOException("Model contains no triangles: " + file.getFileName());
            return new ModelMesh(parts);
        } catch (IllegalArgumentException | ArithmeticException failure) {
            throw new IOException("Invalid model " + file.getFileName() + ": " + failure.getMessage(), failure);
        } finally {
            aiReleaseImport(scene);
        }
    }

    private static void generateMissingNormals(float[] vertices) {
        for (int i = 0; i < vertices.length; i += 24) {
            double ax = (double) vertices[i + 8] - vertices[i];
            double ay = (double) vertices[i + 9] - vertices[i + 1];
            double az = (double) vertices[i + 10] - vertices[i + 2];
            double bx = (double) vertices[i + 16] - vertices[i];
            double by = (double) vertices[i + 17] - vertices[i + 1];
            double bz = (double) vertices[i + 18] - vertices[i + 2];
            double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0 && Double.isFinite(length)) { nx /= length; ny /= length; nz /= length; }
            else { nx = 0; ny = 1; nz = 0; }
            for (int j = i + 5; j < i + 24; j += 8) {
                double magnitude = (double) vertices[j] * vertices[j]
                        + (double) vertices[j + 1] * vertices[j + 1] + (double) vertices[j + 2] * vertices[j + 2];
                if (!(magnitude > 0) || !Double.isFinite(magnitude)) {
                    vertices[j] = (float) nx; vertices[j + 1] = (float) ny; vertices[j + 2] = (float) nz;
                }
            }
        }
    }
}
