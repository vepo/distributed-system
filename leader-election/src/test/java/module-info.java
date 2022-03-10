module io.vepo.distributedsystem.leaderelection.test {
    requires io.vepo.distributedsystem.leaderelection;
    exports io.vepo.distributedsystem.leaderelection.test;

    requires transitive org.junit.jupiter.engine;
    requires transitive org.junit.jupiter.api;

    opens io.vepo.distributedsystem.leaderelection.test.commands to org.junit.platform.commons;
}
