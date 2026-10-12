.class public final Lcom/cidercollective/cider/sidcloud/SidGlue;
.super Ljava/lang/Object;


# direct methods
.method public static context()Landroid/content/Context;
    .locals 1

    invoke-static {}, {{T_MLKIT_CONTEXT}}->{{N_MLKIT_CONTEXT_GET}}(){{T_MLKIT_CONTEXT}}

    move-result-object v0

    invoke-virtual {v0}, {{T_MLKIT_CONTEXT}}->{{N_MLKIT_CONTEXT_APP}}()Landroid/content/Context;

    move-result-object v0

    return-object v0
.end method

.method public static enabled()Z
    .locals 1

    :try_start
    invoke-static {}, Lcom/cidercollective/cider/sidcloud/SidGlue;->context()Landroid/content/Context;

    move-result-object v0

    invoke-static {v0}, Lcom/cidercollective/cider/sidcloud/CloudSid;->enabled(Landroid/content/Context;)Z

    move-result v0
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch

    return v0

    :catch
    move-exception v0

    const/4 v0, 0x0

    return v0
.end method

# JSON schema text for the requested response class; null when it can't be derived.
.method public static schema({{T_KCLASS}})Ljava/lang/String;
    .locals 2

    :try_start
    invoke-static {p0}, {{M_SCHEMA_KEY}}

    move-result-object v0

    invoke-virtual {v0}, {{M_SCHEMA_NODE}}

    move-result-object v0

    const-string v1, ""

    invoke-static {v1, v0}, {{M_SCHEMA_TEXT}}

    move-result-object v0
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch

    return-object v0

    :catch
    move-exception v0

    const/4 v0, 0x0

    return-object v0
.end method

# Replaces the ML Kit generate call: flattens the request's text parts into one prompt, asks Gemini,
# and wraps the reply (or the failure) in Cider's own response/error types.
.method public static generate({{T_I23}}{{T_KCLASS}})Ljava/lang/Object;
    .locals 9

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v1, p0, {{F_I23_CONTENTS}}

    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :next_content
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :contents_done

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, {{T_IN1}}

    iget-object v2, v2, {{F_IN1_PARTS}}

    invoke-interface {v2}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :next_part
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v3

    if-eqz v3, :next_content

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    instance-of v4, v3, {{T_MM8}}

    if-eqz v4, :next_part

    check-cast v3, {{T_MM8}}

    iget-object v3, v3, {{F_MM8_TEXT}}

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, "\n"

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :next_part

    :contents_done
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    iget v1, p0, {{F_I23_TEMPERATURE}}

    const/4 v2, 0x0

    if-eqz p1, :no_schema

    invoke-static {p1}, Lcom/cidercollective/cider/sidcloud/SidGlue;->schema({{T_KCLASS}})Ljava/lang/String;

    move-result-object v2

    :no_schema
    :try_start
    invoke-static {}, Lcom/cidercollective/cider/sidcloud/SidGlue;->context()Landroid/content/Context;

    move-result-object v3

    invoke-static {v3, v0, v1, v2}, Lcom/cidercollective/cider/sidcloud/CloudSid;->generate(Landroid/content/Context;Ljava/lang/String;FLjava/lang/String;)Lcom/cidercollective/cider/sidcloud/CloudSid$Result;

    move-result-object v3
    :try_end
    .catch Lcom/cidercollective/cider/sidcloud/CloudSidException; {:try_start .. :try_end} :handler

    iget v4, v3, Lcom/cidercollective/cider/sidcloud/CloudSid$Result;->finishReason:I

    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    iget-object v5, v3, Lcom/cidercollective/cider/sidcloud/CloudSid$Result;->text:Ljava/lang/String;

    new-instance v6, {{T_VT0}}

    invoke-direct {v6, v4, v5}, {{M_VT0_INIT}}

    new-instance v7, Ljava/util/ArrayList;

    invoke-direct {v7}, Ljava/util/ArrayList;-><init>()V

    invoke-virtual {v7, v6}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    new-instance v8, {{T_J23}}

    invoke-direct {v8, v7}, {{M_J23_INIT}}

    return-object v8

    :handler
    move-exception v3

    new-instance v4, {{T_E23}}

    invoke-virtual {v3}, Lcom/cidercollective/cider/sidcloud/CloudSidException;->getMessage()Ljava/lang/String;

    move-result-object v5

    iget v6, v3, Lcom/cidercollective/cider/sidcloud/CloudSidException;->code:I

    invoke-direct {v4, v6, v5, v3}, {{M_E23_INIT}}

    throw v4
.end method
