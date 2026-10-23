/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.subscription.preflight;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 已登录用户仅预检自己提交的批次，不读取其他企业数据。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
@RestController
@RequestMapping("/api/preflight")
public class PreflightController {
  private final CsvPreflight service;
  private final cn.zhuatech.subscription.Auth auth;

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  public PreflightController(CsvPreflight service, cn.zhuatech.subscription.Auth auth) {
    this.service = service;
    this.auth = auth;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private void authorize(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    auth.current(authorization);
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @GetMapping("/schema")
  public ResponseEntity<?> schema(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authorize(authorization);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(Map.of("success", true, "data", service.schema()));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @PostMapping("/preview")
  public ResponseEntity<?> preview(
      @RequestBody String raw,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authorize(authorization);
    try {
      return ResponseEntity.ok()
          .cacheControl(CacheControl.noStore())
          .body(Map.of("success", true, "message", "预检完成；未写入业务库", "data", service.request(raw)));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest()
          .cacheControl(CacheControl.noStore())
          .body(Map.of("success", false, "message", ex.getMessage()));
    }
  }
}
