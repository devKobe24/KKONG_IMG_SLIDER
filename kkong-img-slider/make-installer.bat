@echo off
REM ✅ JDK 17 이상이 설치되어 있어야 합니다
REM ✅ 프로젝트 루트에서 실행하세요

set JAR_NAME=kkong-img-slider-1.0-SNAPSHOT.jar
set MAIN_CLASS=com.kobe.kkong.img.slider.ImageSlideshowWithFadeAndResize
set APP_NAME=이미지슬라이드쇼
set ICON=icon.ico
set VERSION=1.0.0

REM 생성된 .jar가 있는지 확인
IF NOT EXIST "build\libs\%JAR_NAME%" (
  echo [ERROR] %JAR_NAME% 파일이 없습니다. 먼저 gradlew build 를 실행하세요.
  exit /b
)

REM 설치 파일 생성
jpackage ^
  --input build\libs ^
  --name %APP_NAME% ^
  --main-jar %JAR_NAME% ^
  --main-class %MAIN_CLASS% ^
  --type exe ^
  --icon %ICON% ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --dest dist ^
  --app-version %VERSION%

echo [DONE] Setup.exe 생성 완료: dist\%APP_NAME%-Installer.exe
pause