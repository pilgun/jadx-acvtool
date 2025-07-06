# jadx-acvtool

This plugin allows opening [ACVTool](https://github.com/pilgun/acvtool) instruction coverage report from JaDX-GUI classes.

### Installation

This plugin requires [JADX](https://github.com/skylot/jadx) 1.5.2 or greater.

- jadx-gui: in menu Plugins go to Install plugin and select you plugin jar

### Usage

- Setup the ACVTool report directory in the plugin menu `Preferences -> ACVTool Plugin` (e.g. /Users/user/acvtool/acvtool_working_dir/report).
- Run `Plugins -> Re-scan ACV Report Classes` every time when you upgraded the acv report directory.

### Build the plugin

- Add the dependency (jadx-1.5.2-all.jar)
- `./gradlew jar`

# License

Copyright 2024, Aleksandr Pilgun

Licensed under the Apache License, Version 2.0 (the "License");
you may not use files from this jadx-acvtool repository except in compliance 
with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.