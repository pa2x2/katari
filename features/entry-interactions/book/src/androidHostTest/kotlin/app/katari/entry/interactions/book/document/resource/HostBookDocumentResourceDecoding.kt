package app.katari.entry.interactions.book.document.resource

/**
 * Android's decoding for host tests, where the image decoder's native library can't load: raster images go through
 * BitmapFactory, as the formats the image decoder doesn't read do on a device.
 */
internal object HostBookDocumentResourceDecoding : BookDocumentResourceDecoding by AndroidBookDocumentResourceDecoding {
    override fun readRasterSize(bytes: ByteArray) = readBitmapFactorySize(bytes)

    override fun decodeRaster(bytes: ByteArray, sampleSize: Int) = decodeWithBitmapFactory(bytes, sampleSize)
}
