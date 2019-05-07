package(
    default_testonly = True,
    default_visibility = ["//visibility:public"],
)

load("@batfish//skylark:junit.bzl", "junit_tests")

junit_tests(
    name = "orphaned_tests",
    srcs = glob([
        "**/*Test.java",
    ]),
    deps = [],
)
