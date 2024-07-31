package org.nkjmlab.go.javalin.handler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nkjmlab.go.javalin.handler.GoGetHandler.GoViewHandler;
import org.nkjmlab.util.java.lang.JavaSystemProperties;
import org.nkjmlab.util.java.lang.PropertiesUtils;
import org.nkjmlab.util.java.web.ViewModel.Builder;

import io.javalin.http.Context;
import j2html.TagCreator;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;

public class AdminViewHandler implements GoViewHandler {

  private static final Map<String, String> gitProps =
      PropertiesUtils.load("/git.properties").entrySet().stream()
          .sorted(Comparator.comparing(en -> en.getKey().toString()))
          .collect(
              Collectors.toMap(
                  en -> en.getKey().toString(),
                  en -> en.getValue().toString(),
                  (v1, v2) -> v1,
                  LinkedHashMap::new));

  @Override
  public Function<String, Consumer<Builder>> apply(Context ctx) {
    return filePath ->
        model -> {
          DivTag gitInfo =
              TagCreator.div(
                  gitProps.entrySet().stream()
                      .map(en -> createKeyBadgeValuePre(en.getKey(), en.getValue()))
                      .toArray(DomContent[]::new));
          model.put("gitInfo", gitInfo);

          DivTag javaInfo =
              TagCreator.div(
                  JavaSystemProperties.create().getProperties().entrySet().stream()
                      .sorted(Comparator.comparing(en -> en.getKey()))
                      .map(en -> createKeyBadgeValuePre(en.getKey().name(), en.getValue()))
                      .toArray(DomContent[]::new));
          model.put("javaInfo", javaInfo);

          ctx.render("admin.html", model.build());
        };
  }

  private DomContent createKeyBadgeValuePre(String name, Object value) {
    return createKeyBadgeValue(
        name, value == null ? null : TagCreator.pre(value.toString()).withClass("mb-1"));
  }

  private DomContent createKeyBadgeValue(String name, DomContent value) {
    return TagCreator.div(
        TagCreator.span(name).withClass("badge bg-primary-subtle text-dark px-2 py-1"),
        value == null || value.toString().length() == 0
            ? TagCreator.div("null").withClass("fst-italic text-muted small mb-1")
            : value);
  }
}
