package com.harish.configuration;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioInitializer implements CommandLineRunner {

    private static final String TEMPLATE_BUCKET = "starter-projects";
    private static final String TARGET_BUCKET = "projects";
    private static final String TEMPLATE_PREFIX = "react-vite-tailwind-daisyui-starter/";
    private final MinioClient minioClient;

    @Override
    public void run(String... args) {
        try {
            ensureBucket(TEMPLATE_BUCKET);
            ensureBucket(TARGET_BUCKET);

            // Check if the template has files
            boolean hasFiles = false;
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(TEMPLATE_BUCKET)
                            .prefix(TEMPLATE_PREFIX)
                            .recursive(true)
                            .maxKeys(1)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.objectName().endsWith("/")) {
                    hasFiles = true;
                    break;
                }
            }

            if (!hasFiles) {
                log.warn("========================================================");
                log.warn("  STARTER TEMPLATE IS MISSING!");
                log.warn("  Bucket '{}' has no files under prefix '{}'.", TEMPLATE_BUCKET, TEMPLATE_PREFIX);
                log.warn("  Seeding a default React+Vite+Tailwind+DaisyUI template...");
                log.warn("========================================================");
                seedDefaultTemplate();
                log.info("Default starter template seeded successfully.");
            } else {
                log.info("Starter template found in bucket '{}' with prefix '{}'.", TEMPLATE_BUCKET, TEMPLATE_PREFIX);
            }

        } catch (Exception e) {
            log.error("MinIO initialization failed: {}. Projects may be created without template files.", e.getMessage(), e);
        }
    }

    private void ensureBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            log.info("Creating MinIO bucket: {}", bucketName);
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }
    }

    private void seedDefaultTemplate() throws Exception {
        Map<String, String> templateFiles = buildDefaultTemplateFiles();
        for (Map.Entry<String, String> entry : templateFiles.entrySet()) {
            String objectKey = TEMPLATE_PREFIX + entry.getKey();
            byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(TEMPLATE_BUCKET)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(guessContentType(entry.getKey()))
                            .build()
            );
            log.info("Seeded template file: {}", objectKey);
        }
    }

    private Map<String, String> buildDefaultTemplateFiles() {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("package.json", """
                {
                  "name": "react-vite-tailwind-daisyui-starter",
                  "private": true,
                  "version": "1.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "tsc -b && vite build",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "react": "^18.3.1",
                    "react-dom": "^18.3.1",
                    "lucide-react": "^0.460.0"
                  },
                  "devDependencies": {
                    "@types/react": "^18.3.12",
                    "@types/react-dom": "^18.3.1",
                    "@vitejs/plugin-react": "^4.3.4",
                    "autoprefixer": "^10.4.20",
                    "daisyui": "^5.0.0",
                    "postcss": "^8.4.49",
                    "tailwindcss": "^4.0.0",
                    "typescript": "~5.6.2",
                    "vite": "^6.0.1",
                    "@tailwindcss/vite": "^4.0.0"
                  }
                }
                """);

        files.put("tsconfig.json", """
                {
                  "compilerOptions": {
                    "target": "ES2020",
                    "useDefineForClassFields": true,
                    "lib": ["ES2020", "DOM", "DOM.Iterable"],
                    "module": "ESNext",
                    "skipLibCheck": true,
                    "moduleResolution": "bundler",
                    "allowImportingTsExtensions": true,
                    "isolatedModules": true,
                    "moduleDetection": "force",
                    "noEmit": true,
                    "jsx": "react-jsx",
                    "strict": true,
                    "noUnusedLocals": false,
                    "noUnusedParameters": false,
                    "noFallthroughCasesInSwitch": true,
                    "paths": { "@/*": ["./src/*"] },
                    "baseUrl": "."
                  },
                  "include": ["src"]
                }
                """);

        files.put("vite.config.ts", """
                import { defineConfig } from 'vite'
                import react from '@vitejs/plugin-react'
                import tailwindcss from '@tailwindcss/vite'
                import path from 'path'
                
                export default defineConfig({
                  plugins: [react(), tailwindcss()],
                  resolve: {
                    alias: {
                      '@': path.resolve(__dirname, './src'),
                    },
                  },
                })
                """);

        files.put("postcss.config.js", """
                export default {
                  plugins: {
                    autoprefixer: {},
                  },
                }
                """);

        files.put("index.html", """
                <!doctype html>
                <html lang="en" data-theme="dark">
                  <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>My App</title>
                  </head>
                  <body>
                    <div id="root"></div>
                    <script type="module" src="/src/main.tsx"></script>
                  </body>
                </html>
                """);

        files.put("src/main.tsx", """
                import React from 'react'
                import ReactDOM from 'react-dom/client'
                import App from './App'
                import './index.css'
                
                ReactDOM.createRoot(document.getElementById('root')!).render(
                  <React.StrictMode>
                    <App />
                  </React.StrictMode>,
                )
                """);

        files.put("src/index.css", """
                @import "tailwindcss";
                @plugin "daisyui";
                """);

        files.put("src/App.tsx", """
                import { Heart } from 'lucide-react'
                
                function App() {
                  return (
                    <div className="min-h-screen bg-base-200 flex items-center justify-center">
                      <div className="card bg-base-100 shadow-xl p-8 max-w-md text-center">
                        <h1 className="text-3xl font-bold mb-4">Welcome to Your App</h1>
                        <p className="text-base-content/70 mb-6">
                          Start building something amazing with React, Tailwind CSS, and DaisyUI.
                        </p>
                        <div className="flex justify-center gap-2">
                          <button className="btn btn-primary">
                            <Heart className="w-4 h-4" />
                            Get Started
                          </button>
                        </div>
                      </div>
                    </div>
                  )
                }
                
                export default App
                """);

        files.put("src/vite-env.d.ts", """
                /// <reference types="vite/client" />
                """);

        return files;
    }

    private String guessContentType(String path) {
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".ts") || path.endsWith(".tsx")) return "text/typescript";
        if (path.endsWith(".js") || path.endsWith(".jsx")) return "text/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".html")) return "text/html";
        return "text/plain";
    }
}
