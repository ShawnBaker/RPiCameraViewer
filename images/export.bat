@echo off

setlocal

if "%2"=="" goto usage
if "%3"=="" goto good_to_go
goto usage
:good_to_go

set TARGET_OS=%1
set TARGET_PATH=%2

if exist %TARGET_PATH% goto path_ok
echo Path doesn't exist: %TARGET_PATH%
goto usage
:path_ok

if "%TARGET_OS%"=="android" goto android
if "%TARGET_OS%"=="ios" goto ios
if "%TARGET_OS%"=="windows" goto windows
goto usage

REM ================ android ===============
:android

set ANDROID_PATH=%TARGET_PATH%\app\src\main\res
if exist %ANDROID_PATH% goto path_ok
echo Android path doesn't exist: %ANDROID_PATH%
goto usage
:path_ok

call :export_android close_button
call :export_android dark_chevron
call :export_android snapshot_button
REM call :export_android video_button
call :export_android white_plus
call :export_android white_x
goto :eof

REM ================== ios =================
:ios

set IOS_PATH=%TARGET_PATH%\RPiCameraViewer\Assets.xcassets
if exist %IOS_PATH% goto path_ok
echo iOS path doesn't exist: %IOS_PATH%
goto usage
:path_ok

call :export_ios close_button CloseButton
call :export_ios snapshot_button SnapshotButton
goto :eof

REM ================ windows ===============
:windows

set WINDOWS_PATH=%TARGET_PATH%\Assets
if exist %WINDOWS_PATH% goto path_ok
echo Windows path doesn't exist: %WINDOWS_PATH%
goto usage
:path_ok

call :export_windows chevron chevron
call :export_windows chevron_pressed chevron
call :export_windows chevron_disabled chevron
call :export_windows close_button close_button
call :export_windows close_disabled_button close_button
call :export_windows close_pressed_button close_button
call :export_windows snapshot_button snapshot_button
call :export_windows snapshot_disabled_button snapshot_button
call :export_windows snapshot_pressed_button snapshot_button
call :export_windows trash trash
call :export_windows trash_pressed trash
call :export_windows trash_disabled trash
goto :eof

REM ============= export functions =============
:export_android
inkscape -f images.svg -i %~1 -j -d 90 -e %ANDROID_PATH%\drawable-mdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 135 -e %ANDROID_PATH%\drawable-hdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 180 -e %ANDROID_PATH%\drawable-xhdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 270 -e %ANDROID_PATH%\drawable-xxhdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 360 -e %ANDROID_PATH%\drawable-xxxhdpi\%~1.png
goto :eof

:export_ios
set FILE_NAME=%IOS_PATH%\%~2.imageset\%~1
inkscape -f images.svg -i %~1 -j -d 135 -e %FILE_NAME%-1.png
inkscape -f images.svg -i %~1 -j -d 270 -e %FILE_NAME%-2.png
inkscape -f images.svg -i %~1 -j -d 405 -e %FILE_NAME%-3.png
goto :eof

:export_windows
set FILE_NAME=%WINDOWS_PATH%\%~2\%~1.scale
inkscape -f images.svg -i %~1 -j -d 90 -e %FILE_NAME%-100.png
inkscape -f images.svg -i %~1 -j -d 112.5 -e %FILE_NAME%-125.png
inkscape -f images.svg -i %~1 -j -d 135 -e %FILE_NAME%-150.png
inkscape -f images.svg -i %~1 -j -d 180 -e %FILE_NAME%-200.png
inkscape -f images.svg -i %~1 -j -d 202.5 -e %FILE_NAME%-225.png
REM inkscape -f images.svg -i %~1 -j -d 225 -e %FILE_NAME%-250.png
inkscape -f images.svg -i %~1 -j -d 270 -e %FILE_NAME%-300.png
REM inkscape -f images.svg -i %~1 -j -d 315 -e %FILE_NAME%-350.png
inkscape -f images.svg -i %~1 -j -d 360 -e %FILE_NAME%-400.png
goto :eof

REM ================= usage ================
:usage
echo.
echo Usage^: %0 os path
echo    os - android, ios, windows
echo    path - full path to the application folder
echo.
goto :eof
