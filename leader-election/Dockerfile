FROM openjdk:17-alpine

ADD target/libs                /opt/libs
ADD target/leader-election.jar /opt/leader-election.jar
##java --module-path target/libs/ --add-modules org.slf4j -jar target/leader-election.jar
CMD ["java", "--module-path", "/opt/libs", "--add-modules", "org.slf4j", "-jar", "/opt/leader-election.jar"]