Helwan-Djibril MIMOUNI

MasterServer

Compilation :
javac -d out src/main/java/MasterServer.java
Exécution :
java -cp out src/main/java/MasterServer.java 1234 
(ou autre port)

Server (mieux 3, mais si - ou + changer serverdispatch)

Compilation :
javac -d out src/main/java/ServerCentralPush.java
Exécution :
java -cp out src/main/java/ServerCentralPush.java 12346 1234
(port dans liste serverdispatch + port master)

MasterServer

Compilation :
javac -d out src/main/java/ServerDispatch.java
Exécution :
java -cp out src/main/java/ServerDispatch.java

Client

Compilation :
javac --module-path "C:\javafx-sdk-21.0.2\lib" --add-modules javafx.controls,javafx.fxml -d out src/main/java/GUIClient.java src/main/java/ClientController.java
Exécution :
java --module-path "C:\javafx-sdk-21.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp out GUIClient