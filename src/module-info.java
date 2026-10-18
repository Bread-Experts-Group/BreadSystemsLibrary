open module BreadSystemsLibrary {
    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires jdk.dynalink;

    requires java.xml;
    requires java.net.http;

    exports org.bread_experts_group.bread_systems_library.model;
    exports org.bread_experts_group.bread_systems_library.generic;
}