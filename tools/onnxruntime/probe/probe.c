// Loads a model the way the app does (CPU, ORT_ENABLE_ALL), optionally saves the graph as optimized for this CPU,
// runs it once on deterministic inputs and prints output checksums.
//
// Usage: probe <model.onnx> <optimized-output.onnx|-> [input]...
// Each input, in model order, is `f:<dims>` (float) or `i=<value>:<dims>` (int64 filled with value), with dims
// separated by commas. Without inputs the model is only loaded.
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "onnxruntime_c_api.h"

#define MAX_TENSORS 8
#define MAX_RANK 8

static const OrtApi *api;

static void check(OrtStatus *status, const char *step) {
    if (!status) return;
    printf("ERROR %s: %s\n", step, api->GetErrorMessage(status));
    exit(1);
}

static OrtValue *create_input(const char *spec, const OrtMemoryInfo *memory) {
    int is_float = spec[0] == 'f';
    int64_t fill = is_float ? 0 : atoll(spec + 2);
    int64_t shape[MAX_RANK];
    size_t rank = 0, count = 1;
    for (const char *p = strchr(spec, ':') + 1; *p && rank < MAX_RANK;) {
        char *end;
        shape[rank] = strtoll(p, &end, 10);
        count *= (size_t)shape[rank++];
        p = *end == ',' ? end + 1 : end;
    }
    OrtValue *value;
    if (is_float) {
        float *data = malloc(count * sizeof(float));
        for (size_t k = 0; k < count; k++) data[k] = 0.5f + 0.5f * sinf((float)k * 0.37f);
        check(api->CreateTensorWithDataAsOrtValue(memory, data, count * sizeof(float), shape, rank,
                                                  ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &value), "input");
    } else {
        int64_t *data = malloc(count * sizeof(int64_t));
        for (size_t k = 0; k < count; k++) data[k] = fill;
        check(api->CreateTensorWithDataAsOrtValue(memory, data, count * sizeof(int64_t), shape, rank,
                                                  ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64, &value), "input");
    }
    return value;
}

static void print_checksum(const char *name, const OrtValue *output) {
    OrtTensorTypeAndShapeInfo *info;
    check(api->GetTensorTypeAndShape(output, &info), "output info");
    size_t count;
    ONNXTensorElementDataType type;
    check(api->GetTensorShapeElementCount(info, &count), "output count");
    check(api->GetTensorElementType(info, &type), "output type");
    void *raw;
    check(api->GetTensorMutableData((OrtValue *)output, &raw), "output data");
    double sum = 0, weighted = 0;
    for (size_t k = 0; k < count; k++) {
        double x = type == ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT   ? ((float *)raw)[k]
                   : type == ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64 ? (double)((int64_t *)raw)[k]
                                                                 : NAN;
        sum += x;
        weighted += x * (double)(k % 101 + 1);
    }
    printf("OUTPUT %s count=%zu sum=%.6g weighted=%.6g\n", name, count, sum, weighted);
    api->ReleaseTensorTypeAndShapeInfo(info);
}

int main(int argc, char **argv) {
    if (argc < 3) {
        fprintf(stderr, "usage: %s <model.onnx> <optimized-output.onnx|-> [input]...\n", argv[0]);
        return 2;
    }
    api = OrtGetApiBase()->GetApi(ORT_API_VERSION);
    OrtEnv *env;
    check(api->CreateEnv(ORT_LOGGING_LEVEL_WARNING, "probe", &env), "environment");
    OrtSessionOptions *options;
    check(api->CreateSessionOptions(&options), "session options");
    check(api->SetSessionGraphOptimizationLevel(options, ORT_ENABLE_ALL), "optimization level");
    check(api->SetIntraOpNumThreads(options, 2), "threads");
    if (strcmp(argv[2], "-") != 0) check(api->SetOptimizedModelFilePath(options, argv[2]), "optimized output");
    OrtSession *session;
    check(api->CreateSession(env, argv[1], options, &session), "session");
    printf("LOADED %s\n", argv[1]);

    size_t input_count, output_count;
    check(api->SessionGetInputCount(session, &input_count), "input count");
    check(api->SessionGetOutputCount(session, &output_count), "output count");
    if (argc == 3) return 0;
    if ((size_t)(argc - 3) != input_count || input_count > MAX_TENSORS || output_count > MAX_TENSORS) {
        printf("ERROR inputs: the model takes %zu inputs\n", input_count);
        return 1;
    }

    OrtAllocator *allocator;
    check(api->GetAllocatorWithDefaultOptions(&allocator), "allocator");
    OrtMemoryInfo *memory;
    check(api->CreateCpuMemoryInfo(OrtArenaAllocator, OrtMemTypeDefault, &memory), "memory info");
    char *input_names[MAX_TENSORS], *output_names[MAX_TENSORS];
    OrtValue *inputs[MAX_TENSORS], *outputs[MAX_TENSORS] = {0};
    for (size_t i = 0; i < input_count; i++) {
        check(api->SessionGetInputName(session, i, allocator, &input_names[i]), "input name");
        inputs[i] = create_input(argv[3 + i], memory);
    }
    for (size_t i = 0; i < output_count; i++) {
        check(api->SessionGetOutputName(session, i, allocator, &output_names[i]), "output name");
    }
    check(api->Run(session, NULL, (const char *const *)input_names, (const OrtValue *const *)inputs, input_count,
                   (const char *const *)output_names, output_count, outputs), "run");
    for (size_t i = 0; i < output_count; i++) print_checksum(output_names[i], outputs[i]);
    return 0;
}
