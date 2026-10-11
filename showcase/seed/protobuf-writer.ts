/**
 * The protobuf encoding kotlinx.serialization uses for Katari backups: integers and booleans as plain varints (negative
 * values as ten-byte two's complement), doubles as fixed64, strings, byte arrays, and nested messages as
 * length-delimited fields, and lists as repeated fields. Absent values are left out, as the app leaves out defaults.
 */
export class ProtoWriter {
  private readonly chunks: Uint8Array[] = [];

  int(field: number, value: number | bigint | undefined): this {
    if (value === undefined) return this;
    this.tag(field, 0);
    this.varint(BigInt.asUintN(64, BigInt(value)));
    return this;
  }

  bool(field: number, value: boolean | undefined): this {
    return value === undefined ? this : this.int(field, value ? 1 : 0);
  }

  double(field: number, value: number | undefined): this {
    if (value === undefined) return this;
    this.tag(field, 1);
    const bytes = new Uint8Array(8);
    new DataView(bytes.buffer).setFloat64(0, value, true);
    this.chunks.push(bytes);
    return this;
  }

  string(field: number, value: string | undefined): this {
    return value === undefined ? this : this.bytes(field, new TextEncoder().encode(value));
  }

  strings(field: number, values: readonly string[]): this {
    for (const value of values) this.string(field, value);
    return this;
  }

  bytes(field: number, value: Uint8Array | undefined): this {
    if (value === undefined) return this;
    this.tag(field, 2);
    this.varint(BigInt(value.length));
    this.chunks.push(value);
    return this;
  }

  message<T>(field: number, value: T | undefined, encode: (writer: ProtoWriter, value: T) => void): this {
    if (value === undefined) return this;
    const nested = new ProtoWriter();
    encode(nested, value);
    return this.bytes(field, nested.finish());
  }

  messages<T>(field: number, values: readonly T[], encode: (writer: ProtoWriter, value: T) => void): this {
    for (const value of values) this.message(field, value, encode);
    return this;
  }

  finish(): Uint8Array {
    const length = this.chunks.reduce((total, chunk) => total + chunk.length, 0);
    const result = new Uint8Array(length);
    let offset = 0;
    for (const chunk of this.chunks) {
      result.set(chunk, offset);
      offset += chunk.length;
    }
    return result;
  }

  private tag(field: number, wireType: number): void {
    this.varint(BigInt((field << 3) | wireType));
  }

  private varint(value: bigint): void {
    const bytes: number[] = [];
    let rest = value;
    do {
      let byte = Number(rest & 0x7fn);
      rest >>= 7n;
      if (rest !== 0n) byte |= 0x80;
      bytes.push(byte);
    } while (rest !== 0n);
    this.chunks.push(Uint8Array.from(bytes));
  }
}
