# Sourced by the probes: the deterministic inputs probe.c runs each model of the provider with, by the file name
# fetch-models.py gives the model.
model_inputs() {
    case $1 in
        *decoder*) echo "i=2:1,8 f:1,197,768" ;;
        *detector*) echo "f:1,3,640,640 i=640:1,2" ;;
        *encoder*) echo "f:1,3,224,224" ;;
        det*) echo "f:1,3,640,640" ;;
        rec*) echo "f:1,3,48,320" ;;
        *) echo "no inputs known for $1" >&2; return 2 ;;
    esac
}
