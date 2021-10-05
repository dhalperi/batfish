load("@rules_java//java:defs.bzl", "java_library")
load("@batfish//skylark:pmd_test.bzl", "pmd_test")

java_library(
    name = "guava",
    srcs = glob(
        [
            "*.java",
        ],
    ),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
    ],
)

pmd_test(
    name = "guava_pmd",
    lib = ":guava",
)
