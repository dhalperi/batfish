load("@rules_java//java:defs.bzl", "java_library")
load("@batfish//skylark:pmd_test.bzl", "pmd_test")

java_library(
    name = "serialization",
    srcs = glob(
        [
            "*.java",
        ],
    ),
    visibility = ["//visibility:public"],
    deps = [
        "//projects/batfish-common-protocol/src/main/java/org/batfish/common/util/serialization/guava",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
    ],
)

pmd_test(
    name = "serialization_pmd",
    lib = ":serialization",
)
