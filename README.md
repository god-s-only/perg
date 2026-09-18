# Perg Converter

100% offline file converter for Android. Pick any supported file, convert it on-device, find the result in **Download / Perg**. No accounts, no network, your files never leave your phone.

## Features (v1.0.0)

- Image (JPEG, PNG, WEBP) to PDF
- Text (.txt) to PDF and Word (.docx)
- Word (.docx) to PDF and text
- PDF to PNG / JPEG images (every page), text and Word
- Rename the output right from the result dialog
- Open the converted file straight from the app
- Files land in Download / Perg with the source file's name
- Material 3 UI, dark-mode ready, single-screen flow

## Offline conversion matrix

| From \ To | PDF | PNG | JPEG | DOCX | TXT |
|---|:---:|:---:|:---:|:---:|:---:|
| JPEG | yes | - | - | - | - |
| PNG | yes | - | - | - | - |
| WEBP | yes | - | - | - | - |
| PDF | - | yes | yes | yes | yes |
| DOCX | yes | - | - | - | yes |
| TXT | yes | - | - | yes | - |

Everything runs on-device with `PdfDocument`, `PdfRenderer`, `BitmapFactory`, Apache POI and PdfBox-Android.

## Architecture

Clean Architecture + MVI, one rule enforced throughout: the UI never calls ViewModel methods, it only sends `onEvent`, and the data layer is the single source of truth behind a domain interface.

```
presentation (Compose + HiltViewModel, onEvent only)
    |
domain (models, ConverterRepository contract, ConvertFileUseCase — pure Kotlin)
    |
data (offline converters, ConverterRepositoryImpl, Hilt @Binds module)
```

- Dependency injection with Hilt (`@HiltAndroidApp`, `@Inject` constructors, `@Binds` for the repo)
- Async with Kotlin coroutines (`Flow<ConversionJob>` streams QUEUED → RUNNING → SUCCEEDED / FAILED with progress)
- Min SDK 26, target/compile SDK 36, Java 17, AGP 9, Hilt 2.60.1

## Project structure

```
app/src/main/java/com/perg/converter/
  ConverterApplication.kt        Hilt entry point
  MainActivity.kt                @AndroidEntryPoint host
  Navigation.kt                  Navigation 3 graph
  domain/model/ConversionJob.kt  formats, job, offline capability matrix
  domain/repository/             ConverterRepository contract (+ rename)
  domain/usecase/                ConvertFileUseCase (capability gate)
  data/converter/                ImageToPdf, TextToPdf, PdfToImage engines
  data/repository/               ConverterRepositoryImpl (saves to Download/Perg)
  di/ConverterModule.kt          @Binds module
  presentation/converter/        ConverterEvent / ConverterState / ViewModel / Screen
```

## Build

```powershell
./gradlew :app:assembleDebug
```

A signed release APK is built with your own keystore (never committed to this repo):

```powershell
./gradlew :app:assembleRelease `
  "-Pandroid.injected.signing.store.file=<path-to-keystore>" `
  "-Pandroid.injected.signing.store.password=<password>" `
  "-Pandroid.injected.signing.key.alias=<alias>" `
  "-Pandroid.injected.signing.key.password=<password>"
```

## Roadmap

- Conversion history screen
- Batch / multi-file conversion
- Per-file quality and page-size options
