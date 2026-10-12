.class public final {{T_KQ7}}
.super Ljava/lang/Object;


# Capability probe. With a cloud key configured: structured output on, prefix cache off, 8192-token budget.
.method public final {{N_KQ7_CAPS}}({{T_L0C}}{{T_CONT_IMPL}})Ljava/lang/Object;
    .locals 5

    invoke-static {}, Lcom/cidercollective/cider/sidcloud/SidGlue;->enabled()Z

    move-result v0

    if-eqz v0, :orig

    new-instance v0, {{T_TO7}}

    const/4 v1, 0x0

    const/4 v2, 0x1

    const/4 v3, 0x0

    const/16 v4, 0x2000

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-direct {v0, v1, v2, v3, v4}, {{M_TO7_INIT}}

    return-object v0

    :orig
    invoke-virtual {p0, p1, p2}, {{T_KQ7}}->{{N_KQ7_CAPS}}$cloudOrig({{T_L0C}}{{T_CONT_IMPL}})Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method

# Warmup downloads/loads Nano; nothing to do for the cloud.
.method public final {{N_KQ7_WARMUP}}({{T_CONT_IMPL}})Ljava/lang/Object;
    .locals 1

    invoke-static {}, Lcom/cidercollective/cider/sidcloud/SidGlue;->enabled()Z

    move-result v0

    if-eqz v0, :orig

    sget-object v0, {{F_UNIT_INSTANCE}}

    return-object v0

    :orig
    invoke-virtual {p0, p1}, {{T_KQ7}}->{{N_KQ7_WARMUP}}$cloudOrig({{T_CONT_IMPL}})Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method
