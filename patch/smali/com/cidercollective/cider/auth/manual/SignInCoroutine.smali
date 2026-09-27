.class public final Lcom/cidercollective/cider/auth/manual/SignInCoroutine;
.super Lc/a27;
.implements Lc/zh2;


# instance fields
.field private final activity:Landroid/app/Activity;

.field public label:I

.field private final token:Ljava/lang/String;


# direct methods
.method public constructor <init>(Landroid/app/Activity;Ljava/lang/String;Lc/qb1;)V
    .locals 1

    const/4 v0, 0x2

    invoke-direct {p0, v0, p3}, Lc/a27;-><init>(ILc/qb1;)V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->activity:Landroid/app/Activity;

    iput-object p2, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->token:Ljava/lang/String;

    return-void
.end method


# virtual methods
.method public final p(Lc/qb1;Ljava/lang/Object;)Lc/qb1;
    .locals 3

    new-instance v0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;

    iget-object v1, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->activity:Landroid/app/Activity;

    iget-object v2, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->token:Ljava/lang/String;

    invoke-direct {v0, v1, v2, p1}, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;-><init>(Landroid/app/Activity;Ljava/lang/String;Lc/qb1;)V

    return-object v0
.end method

.method public final m(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    .locals 2

    check-cast p2, Lc/qb1;

    invoke-virtual {p0, p2, p1}, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->p(Lc/qb1;Ljava/lang/Object;)Lc/qb1;

    move-result-object v0

    check-cast v0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;

    sget-object v1, Lc/pl7;->a:Lc/pl7;

    invoke-virtual {v0, v1}, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->s(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    return-object v0
.end method

.method public final s(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 8

    sget-object v0, Lc/bd1;->l:Lc/bd1;

    sget-object v1, Lc/pl7;->a:Lc/pl7;

    iget v2, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->label:I

    packed-switch v2, :switch_data_0

    goto :case0

    :case1
    invoke-static {p1}, Lc/g38;->i(Ljava/lang/Object;)V

    goto :after_probe

    :case2
    invoke-static {p1}, Lc/g38;->i(Ljava/lang/Object;)V

    goto :after_token_edit

    :case3
    invoke-static {p1}, Lc/g38;->i(Ljava/lang/Object;)V

    goto :after_storefront_query

    :case4
    invoke-static {p1}, Lc/g38;->i(Ljava/lang/Object;)V

    goto :finish_step

    :case0
    sget-object v3, Lc/cs;->a:Lc/cs;

    iget-object v4, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->token:Ljava/lang/String;

    const/4 v5, 0x1

    iput v5, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->label:I

    invoke-virtual {v3, v4, p0}, Lc/cs;->t0(Ljava/lang/String;Lc/rb1;)Ljava/lang/Enum;

    move-result-object p1

    if-eq p1, v0, :return_susp

    goto :after_probe

    :after_probe
    check-cast p1, Lc/dp;

    sget-object v3, Lc/dp;->l:Lc/dp;

    if-eq p1, v3, :cond_valid

    iget-object v3, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->activity:Landroid/app/Activity;

    new-instance v4, Lcom/cidercollective/cider/auth/manual/SignInCoroutine$InvalidTokenRunnable;

    invoke-direct {v4, v3}, Lcom/cidercollective/cider/auth/manual/SignInCoroutine$InvalidTokenRunnable;-><init>(Landroid/app/Activity;)V

    invoke-virtual {v3, v4}, Landroid/app/Activity;->runOnUiThread(Ljava/lang/Runnable;)V

    return-object v1

    :cond_valid
    iget-object v3, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->token:Ljava/lang/String;

    invoke-static {v3}, Lc/aa6;->c(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    new-instance v4, Lcom/cidercollective/cider/auth/manual/SaveTokenEdit;

    invoke-direct {v4, v3}, Lcom/cidercollective/cider/auth/manual/SaveTokenEdit;-><init>(Ljava/lang/String;)V

    sget-object v5, Lc/aa6;->i:Lc/yf1;

    const/4 v6, 0x2

    iput v6, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->label:I

    invoke-static {v5, v4, p0}, Lc/oa0;->f(Lc/yf1;Lc/zh2;Lc/qb1;)Ljava/lang/Object;

    move-result-object p1

    if-eq p1, v0, :return_susp

    goto :after_token_edit

    :after_token_edit
    sget-object v3, Lc/cs;->a:Lc/cs;

    const/4 v6, 0x3

    iput v6, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->label:I

    invoke-virtual {v3, p0}, Lc/cs;->n0(Lc/rb1;)Ljava/lang/Object;

    move-result-object p1

    if-eq p1, v0, :return_susp

    goto :after_storefront_query

    :after_storefront_query
    instance-of v3, p1, Ljava/lang/String;

    if-eqz v3, :finish_step

    move-object v3, p1

    check-cast v3, Ljava/lang/String;

    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    move-result v4

    if-nez v4, :finish_step

    invoke-static {v3}, Lc/aa6;->c(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    new-instance v4, Lcom/cidercollective/cider/auth/manual/SaveStorefrontEdit;

    invoke-direct {v4, v3}, Lcom/cidercollective/cider/auth/manual/SaveStorefrontEdit;-><init>(Ljava/lang/String;)V

    sget-object v5, Lc/aa6;->i:Lc/yf1;

    const/4 v6, 0x4

    iput v6, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->label:I

    invoke-static {v5, v4, p0}, Lc/oa0;->f(Lc/yf1;Lc/zh2;Lc/qb1;)Ljava/lang/Object;

    move-result-object p1

    if-eq p1, v0, :return_susp

    goto :finish_step

    :finish_step
    invoke-static {}, Lc/mn;->s()V

    iget-object v3, p0, Lcom/cidercollective/cider/auth/manual/SignInCoroutine;->activity:Landroid/app/Activity;

    new-instance v4, Lcom/cidercollective/cider/auth/manual/SignInCoroutine$FinishRunnable;

    invoke-direct {v4, v3}, Lcom/cidercollective/cider/auth/manual/SignInCoroutine$FinishRunnable;-><init>(Landroid/app/Activity;)V

    invoke-virtual {v3, v4}, Landroid/app/Activity;->runOnUiThread(Ljava/lang/Runnable;)V

    return-object v1

    :return_susp
    return-object v0

    :switch_data_0
    .packed-switch 0x0
        :case0
        :case1
        :case2
        :case3
        :case4
    .end packed-switch
.end method
