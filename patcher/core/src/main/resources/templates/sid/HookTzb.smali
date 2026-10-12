.class public final {{T_TZB}}
.super Ljava/lang/Object;


# Nano status aggregator. With a cloud key configured, report Gemini Nano as AVAILABLE (3) so Sid publishes READY.
.method public static {{N_TZB_STATUS}}({{T_TZB}}{{T_CONT_IMPL}})Ljava/lang/Object;
    .locals 1

    invoke-static {}, Lcom/cidercollective/cider/sidcloud/SidGlue;->enabled()Z

    move-result v0

    if-eqz v0, :orig

    const/4 v0, 0x3

    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v0

    return-object v0

    :orig
    invoke-static {p0, p1}, {{T_TZB}}->{{N_TZB_STATUS}}$cloudOrig({{T_TZB}}{{T_CONT_IMPL}})Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method
