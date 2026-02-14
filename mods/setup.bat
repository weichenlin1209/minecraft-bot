@echo off
echo ============================================
echo   AI ChatBot Fabric Mod - 初始設定
echo ============================================
echo.

REM 檢查 Java
java -version 2>nul
if errorlevel 1 (
    echo [錯誤] 找不到 Java！請安裝 Java 17 或更新版本。
    echo 下載: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java 已安裝

REM 檢查 Gradle Wrapper JAR
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [OK] Gradle Wrapper 已存在
    goto :DONE
)

echo.
echo [提示] 缺少 gradle-wrapper.jar
echo.
echo 請執行以下步驟：
echo   1. 從 https://gradle.org/install/ 安裝 Gradle
echo   2. 在此目錄開啟命令提示字元
echo   3. 執行: gradle wrapper
echo.
echo 或者使用 Scoop 安裝:
echo   scoop install gradle
echo   gradle wrapper
echo.
pause
exit /b 1

:DONE
echo.
echo ============================================
echo   環境檢查通過！接下來請執行：
echo ============================================
echo.
echo   1. gradlew.bat build
echo      (首次執行會下載依賴，可能需要幾分鐘)
echo.
echo   2. 編譯完成後，JAR 會在 build\libs\ 資料夾
echo.
echo   3. 將 JAR 放到 Minecraft 伺服器的 mods\ 資料夾
echo.
echo   4. 啟動伺服器後，編輯 config\aichatbot.json
echo      填入你的 OpenAI API Key
echo.
pause
