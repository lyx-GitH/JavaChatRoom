# JaChat (JavaChatRoom)
## Introduction
a multi-user and multi-session online chatroom with Java

## Code Organization
- The ```lib``` contains the MySQL driver .JAR file
- The 



## How to run

Before start this program, PLEASE be sure that :

- **MySQL is INSTALLED** in your computer
- An empty database is available

Then, you need to:
- Change settings in /src/main/java/ChatDatabase/ServerDBConfig.java
- include the driver .JAR file in /lib to your project setting if necessary

To run the server, run *main* in /src/main/java/ChatApp/JaChatServer.java
To run the client, run *main* in /src/main/java/ChatApp/JaChat.java

To close the client, just click the close button
To close the server, type 'exit' to the terminal, then it will be forced to quit
    (Not recommended when there are still user logged in!)

Enjoy!