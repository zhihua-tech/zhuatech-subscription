/* 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.subscription.preflight;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * 订阅有效期按闭区间核对，续期相邻日期允许。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
class PreflightDomainTest {
  private final CsvPreflight service = new CsvPreflight();
  private final String header = "subscription_no,customer,seats,assigned,start_date,end_date\n";

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void nextDayRenewalDoesNotOverlap() {
    var r =
        service.preview(
            header + "A-1,C-1,100,80,2026-11-01,2027-10-31\nA-2,C-1,100,90,2027-11-01,2028-10-31",
            "2026-10-23");
    assertEquals(true, r.get("valid"));
    assertEquals(2, r.get("validRows"));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void sameDayInclusiveBoundaryDoesOverlap() {
    var r =
        service.preview(
            header + "A-1,C-1,100,80,2026-11-01,2027-10-31\nA-2,C-1,100,90,2027-10-31,2028-10-31",
            "2026-10-23");
    assertEquals(false, r.get("valid"));
    assertEquals(2, r.get("invalidRows"));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void differentCustomersCanShareDates() {
    var r =
        service.preview(
            header + "A-1,C-1,100,80,2026-11-01,2027-10-31\nA-2,C-2,100,90,2026-11-01,2027-10-31",
            "2026-10-23");
    assertEquals(true, r.get("valid"));
  }
}
