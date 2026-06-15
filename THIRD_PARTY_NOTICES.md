# Third-Party Notices

This project depends on Android, Kotlin, Jetpack Compose, Hilt, Room, Retrofit,
OkHttp, ML Kit, ONNX Runtime, and related Gradle dependencies. See the Gradle
dependency declarations for exact package coordinates and versions.

The bundled OCR detection model is stored at:

```text
app/src/main/assets/ocr/rapidocr/ch_PP-OCRv4_det_infer.onnx
```

The bundled file name matches the PP-OCRv4 detection model distributed by
RapidOCR. The SWHL/RapidOCR Hugging Face repository lists its license as
Apache-2.0:

```text
https://huggingface.co/SWHL/RapidOCR/tree/main/PP-OCRv4
```

The original PaddleOCR model catalog documents the PP-OCRv4 detection model:

```text
https://www.paddleocr.ai/v2.9/ppocr/model_list.html
```

If you redistribute modified model files or replace the bundled model, re-check
the exact upstream source and license for that file.
