# The system build.sh builds the Linux library in: AlmaLinux 8 (manylinux_2_28), where Microsoft builds its own Linux
# packages of ONNX Runtime. Its glibc 2.28 and libstdc++ (GLIBCXX 3.4.25) are the floor the library then needs, the
# same glibc floor as the desktop app's libarchive and libmpv; gcc-toolset-14 links newer C++ runtime parts statically.
FROM quay.io/pypa/manylinux_2_28_x86_64@sha256:9555ba6c479fa69a2aa80ad1dc57154a60d562b19df159361d9379a02beb1e36

RUN dnf install -y java-17-openjdk-devel && dnf clean all
RUN /opt/python/cp312-cp312/bin/pip install --no-cache-dir --disable-pip-version-check ninja==1.13.2 \
    flatbuffers==25.12.19

ENV PATH=/opt/python/cp312-cp312/bin:$PATH JAVA_HOME=/usr/lib/jvm/java-17-openjdk
