/** A small deterministic generator (mulberry32), so the seed is byte-identical on every run. */
export class SeededRandom {
  private state: number;

  constructor(seed: number) {
    this.state = seed >>> 0;
  }

  /** A number in [0, 1). */
  next(): number {
    this.state = (this.state + 0x6d2b79f5) >>> 0;
    let value = this.state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  }

  /** An integer in [min, max]. */
  between(min: number, max: number): number {
    return min + Math.floor(this.next() * (max - min + 1));
  }

  uuid(): string {
    const hex = Array.from({ length: 32 }, () => this.between(0, 15).toString(16)).join("");
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-4${hex.slice(13, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  }
}
