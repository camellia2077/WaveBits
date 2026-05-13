@echo off
chcp 65001 >nul
set PYTHONIOENCODING=utf-8

cd /d "%~dp0.."
echo Generating English Radar Chart for: Collapse...
python main.py --chart Collapse --lang en

