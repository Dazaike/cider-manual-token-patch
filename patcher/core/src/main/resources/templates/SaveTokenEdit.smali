.class public final Lcom/cidercollective/cider/auth/manual/SaveTokenEdit;
.super Ljava/lang/Object;
.implements {{T_FUNCTION2}}


# instance fields
.field private final encryptedValue:Ljava/lang/String;


# direct methods
.method public constructor <init>(Ljava/lang/String;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/cidercollective/cider/auth/manual/SaveTokenEdit;->encryptedValue:Ljava/lang/String;

    return-void
.end method


# virtual methods
.method public final {{N_FUNCTION2_INVOKE}}(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    .locals 2

    check-cast p1, {{T_MUTABLE_PREFS}}

    sget-object v0, {{F_KEY_MUSIC_USER_TOKEN}}

    iget-object v1, p0, Lcom/cidercollective/cider/auth/manual/SaveTokenEdit;->encryptedValue:Ljava/lang/String;

    invoke-virtual {p1, v0, v1}, {{M_PREFS_SET}}

    sget-object v0, {{F_UNIT_INSTANCE}}

    return-object v0
.end method
