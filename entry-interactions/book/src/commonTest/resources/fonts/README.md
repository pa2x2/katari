Subset of Open Sans Condensed Bold (SIL Open Font License 1.1) covering "Katarié", made with fontTools:

- `fixture.ttf`: `pyftsubset OpenSans-CondensedBold.ttf --text="Katarié" --layout-features=''`
- `fixture.woff`: fontTools (`recalcBBoxes=False, recalcTimestamp=False`), flavor `woff`
- `fixture.woff2`: likewise, flavor `woff2` (glyf and loca transformed)
- `fixture-hmtx.woff2`: `woff2.compress(..., transform_tables={'glyf', 'loca', 'hmtx'})`
