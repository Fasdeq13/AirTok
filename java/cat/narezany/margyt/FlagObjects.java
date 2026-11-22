package cat.narezany.margyt;

final class FlagObjects {
    private FlagObjects() {}

    static Object build(String json, Class<?> type) throws Exception {
        return NativeJson.decode(json, type);
    }
}
