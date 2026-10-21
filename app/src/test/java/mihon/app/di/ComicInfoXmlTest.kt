package mihon.app.di

import io.kotest.matchers.shouldBe
import nl.adaptivity.xmlutil.xmlStreaming
import org.junit.jupiter.api.Test
import tachiyomi.core.metadata.comicinfo.ComicInfo

class ComicInfoXmlTest {

    @Test
    fun `ComicInfo written into an earlier download reads back with every field`() {
        // Written by the downloader into a chapter's CBZ; existing downloads and local entries keep these files across
        // updates of xmlutil and kotlinx-serialization.
        val document =
            """
            <?xml version='1.0' encoding='UTF-8' ?>
            <ComicInfo xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <Title>第6話</Title>
              <Series>ペッパー＆キャロット</Series>
              <Number>6</Number>
              <Summary>カオサの見習い魔女ペッパーと相棒の猫キャロットが、魔法の世界エレヴァで騒動を巻き起こす。</Summary>
              <Writer>David Revoy</Writer>
              <Penciller>David Revoy</Penciller>
              <Genre>Fantasy, Comedy, Adventure</Genre>
              <Web>/pepper-carrot-ja/6</Web>
              <Year>2015</Year>
              <Month>3</Month>
              <Day>28</Day>
              <ty:PublishingStatusTachiyomi xmlns:ty="http://www.w3.org/2001/XMLSchema">Ongoing</ty:PublishingStatusTachiyomi>
              <mh:SourceMihon xmlns:mh="http://www.w3.org/2001/XMLSchema">Pepper&amp;Carrot</mh:SourceMihon>
            </ComicInfo>
            """.trimIndent()

        // The app parses with Android's XmlPullParser, which host tests only have as a stub; xmlutil's own reader feeds
        // the same serialization layer, which is what depends on the xmlutil and kotlinx-serialization versions.
        val reader = xmlStreaming.newGenericReader(document)
        val comicInfo = AppBindings.xml().decodeFromReader(ComicInfo.serializer(), reader)

        comicInfo shouldBe ComicInfo(
            title = ComicInfo.Title("第6話"),
            series = ComicInfo.Series("ペッパー＆キャロット"),
            number = ComicInfo.Number("6"),
            summary = ComicInfo.Summary("カオサの見習い魔女ペッパーと相棒の猫キャロットが、魔法の世界エレヴァで騒動を巻き起こす。"),
            writer = ComicInfo.Writer("David Revoy"),
            penciller = ComicInfo.Penciller("David Revoy"),
            inker = null,
            colorist = null,
            letterer = null,
            coverArtist = null,
            translator = null,
            genre = ComicInfo.Genre("Fantasy, Comedy, Adventure"),
            tags = null,
            web = ComicInfo.Web("/pepper-carrot-ja/6"),
            year = ComicInfo.Year(2015),
            month = ComicInfo.Month(3),
            day = ComicInfo.Day(28),
            publishingStatus = ComicInfo.PublishingStatusTachiyomi("Ongoing"),
            categories = null,
            source = ComicInfo.SourceMihon("Pepper&Carrot"),
        )
    }
}
