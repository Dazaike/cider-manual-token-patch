.class public final {{T_L0C}}
.super Ljava/lang/Object;


# Structured-output availability: always true for the cloud.
.method public final {{N_L0C_STRUCTURED_OK}}({{T_CONT_IMPL}})Ljava/lang/Object;
    .locals 1

    invoke-static {}, Lcom/cidercollective/cider/sidcloud/SidGlue;->enabled()Z

    move-result v0

    if-eqz v0, :orig

    sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    return-object v0

    :orig
    invoke-virtual {p0, p1}, {{T_L0C}}->{{N_L0C_STRUCTURED_OK}}$cloudOrig({{T_CONT_IMPL}})Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method

# Generate content: answered by Gemini over HTTPS when a cloud key is configured.
.method public final {{N_L0C_GENERATE}}({{T_I23}}{{T_KCLASS}}Z{{T_CONT_IMPL}})Ljava/lang/Object;
    .locals 1

    invoke-static {}, Lcom/cidercollective/cider/sidcloud/SidGlue;->enabled()Z

    move-result v0

    if-eqz v0, :orig

    invoke-static {p1, p2}, Lcom/cidercollective/cider/sidcloud/SidGlue;->generate({{T_I23}}{{T_KCLASS}})Ljava/lang/Object;

    move-result-object v0

    return-object v0

    :orig
    invoke-virtual {p0, p1, p2, p3, p4}, {{T_L0C}}->{{N_L0C_GENERATE}}$cloudOrig({{T_I23}}{{T_KCLASS}}Z{{T_CONT_IMPL}})Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method
