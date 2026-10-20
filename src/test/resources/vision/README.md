# Set de evaluación — Filtro de sustancias prohibidas en imágenes (Tarea IA: Visión)

Aquí van las **fotos reales** para medir la calidad del filtro de visión.
El test `VisionEvaluacionTest` las lee automáticamente y calcula
accuracy / precisión / recall / F1.

```
vision/
├── prohibidas/   ← fotos que SÍ deben rechazarse (drogas, armas, medicamentos controlados)
└── permitidas/   ← fotos de productos normales (cuadernos, celulares, ropa, etc.)
```

## Cómo usarlo

1. Copia tus fotos en cada carpeta (`.jpg`, `.jpeg`, `.png` o `.webp`).
2. Configura la API key (una vez por PC):

   ```powershell
   setx OPENAI_API_KEY "sk-..."
   ```

   Cierra y vuelve a abrir la terminal/VS Code después de `setx`.
3. Corre solo este test:

   ```powershell
   .\mvnw.cmd test -Dtest=VisionEvaluacionTest
   ```

   - Sin `OPENAI_API_KEY` o sin fotos, el test se **salta** (no rompe `mvn test`).
   - El reporte imprime una línea por imagen y al final las métricas.

## Afinamiento del umbral

Si hay muchos falsos positivos (productos normales rechazados), **sube** el umbral
`openai.vision-umbral` en `src/main/resources/application.yaml`.
Si hay muchos falsos negativos (sustancias que se cuelan), **bájalo**.
Vuelve a correr el test y compara el F1.

> Recomendación: usa al menos 10 fotos por carpeta para que las métricas sean estables.
