package org.nkjmlab.util.thymeleaf;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

public class TemplateEngineBuilder {

  private String prefix = "";
  private String suffix = "";
  private long cacheTtlMs = 0;
  private Boolean cacheable = null;

  public TemplateEngineBuilder setCacheble(boolean cacheable) {
    this.cacheable = cacheable;
    return this;
  }

  public TemplateEngineBuilder setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  public TemplateEngineBuilder setSuffix(String suffix) {
    this.suffix = suffix;
    return this;
  }

  public TemplateEngineBuilder setTtlMs(long cacheTtlMs) {
    this.cacheTtlMs = cacheTtlMs;
    return this;
  }

  public TemplateEngine build() {
    TemplateEngine templateEngine = new org.thymeleaf.TemplateEngine();
    templateEngine
        .setTemplateResolver(createTemplateResolver(prefix, suffix, cacheTtlMs, cacheable));
    return templateEngine;
  }


  private static ITemplateResolver createTemplateResolver(String prefix, String suffix,
      long cacheTtlMs, Boolean cacheable) {
    final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setPrefix(prefix);
    templateResolver.setSuffix(suffix);
    templateResolver.setCacheTTLMs(cacheTtlMs);
    if (cacheable != null) {
      templateResolver.setCacheable(cacheable);
    }
    return templateResolver;
  }



}
