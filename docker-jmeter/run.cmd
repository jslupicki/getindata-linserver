@echo off

set NAME=jmeter
if "%JMETER_VERSION%" == "" set JMETER_VERSION=5.4
set IMAGE=justb4/jmeter:%JMETER_VERSION%

if "%TARGET_NETWORK%" == "" (
   docker run --cpus=8 --rm --name %NAME% -i -v %cd%:/working-dir -w /working-dir %IMAGE% %*
) else (
  docker run --cpus=8 --rm --name %NAME% -i -v %cd%:/working-dir -w /working-dir --network %TARGET_NETWORK% %IMAGE% %*
)
