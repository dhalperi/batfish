package(default_visibility = ["//visibility:public"])

load("@batfish//skylark:junit.bzl", "junit_tests")

java_binary(
    name = "allinone_main",
    main_class = "org.batfish.allinone.Main",
    runtime_deps = [
        ":allinone",
        "@slf4j_jdk14//:runtime",
    ],
)

java_library(
    name = "allinone",
    srcs = glob([
        "src/main/**/*.java",
    ]),
    resources = glob(["src/main/resources/**"]),
    runtime_deps = [
        "//projects/minesweeper",
        "//projects/question",
    ],
    deps = [
        "//projects/batfish",
        "//projects/batfish-client:client",
        "//projects/batfish-common-protocol:common",
        "//projects/coordinator",
        "@guava//:compile",
        "@jaeger_core//:compile",
    ],
)
