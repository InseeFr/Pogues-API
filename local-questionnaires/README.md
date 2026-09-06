# Local questionnaires

Drop a Pogues `.json` file here (one questionnaire per file).

If the API is already running (`mvn spring-boot:run` or the IntelliJ `Pogues (local)` run config), the file is loaded or replaced immediately. Otherwise it is applied on the next start. Then refresh the UI.

The JSON `id` must be alphanumeric (`qdemo1`, not `q-demo-1`). Files overwrite the same id in the database. Check the API log for `Loaded questionnaire` or `Replaced questionnaire`.
