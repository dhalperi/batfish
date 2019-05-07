package(
    default_testonly = True,
    default_visibility = ["//visibility:public"],
)

load("@batfish//skylark:junit.bzl", "junit_tests")

java_library(
    name = "lib",
    srcs = ["TwoNodeNetworkWithTwoLinks.java"],
    deps = [
        "//projects/batfish",
        "//projects/batfish:batfish_testlib",
        "//projects/batfish-common-protocol:common",
        "@guava//:compile",
        "@junit//:compile",
    ],
)

junit_tests(
    name = "tests",
    srcs = glob([
        "**/*Test.java",
    ]),
    deps = [
        ":lib",
        "//projects/batfish",
        "//projects/batfish-common-protocol:common",
        "//projects/minesweeper",
        "//projects/minesweeper:matchers",
        "//projects/question",
        "@guava//:compile",
        "@hamcrest//:compile",
    ],
)
