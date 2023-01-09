# Hunter

Hunter is a code analysis tool for android app. It can analyze the usage of intelligent model in a given android app. It has two functions:

- (control mode) Generate control json file including model info for further while box test.
- (code mode) Generate Java code to run model to reuse or development.

## Parameters
```
[required] -i --input [path]                Input APK file path.
[required] -o --output [path]               Output directory path.
[required] -m --mode ["code"/"control"]     Work mode for hunter. "code" to generate Java code and "control" to generate control json for white box test.
[required] -p --android-platform [path]     Path of android platform. Hunter need android platform to run.

[optional] -v --verbose                     Turn on verbose mode for more log output.
```

## Quick Start
### Preparation 
- A Java IDE can help you run Hunter easily. Free Intellij IDEA Community Edition is recommended.
- Download Android Platform. If you have installed Android Studio, it has been installed in somewhere like `C:\Users\[username]\AppData\Local\Android\Sdk\platforms`.

### Run Hunter
Open this project in IDEA and input the parameters. Then run the main() in Main class.

#### Example
```
--input "D:\projects\apks\xxxxx.apk"
--output "D:\projects\output"
--android-platform "C:\Users\username\AppData\Local\Android\Sdk\platforms"
--mode control
-v
```
### Output Example
[example_output](example_output)