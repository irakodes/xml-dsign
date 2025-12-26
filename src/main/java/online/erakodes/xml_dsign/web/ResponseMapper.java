package online.erakodes.xml_dsign.web;

import online.erakodes.xml_dsign.model.Response;
import online.erakodes.xml_dsign.model.Result;

import java.util.List;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Response<T> toHttpResponse(Result<T> result) {
        return switch (result) {
            case Result.Ok<T> ok -> Response.success("Operation completed successfully",
                    ok.data(), ok.details());

            case Result.Fail<T> fail -> Response.error(
                    fail.error().getDetail(),
                    fail.error().getErrorCode(),
                    String.valueOf(fail.error().getType()),
                    fail.error().getErrorData() instanceof List<?> list
                            ? (List<String>) list
                            : null,
                    fail.error().getDetail()
            );
        };
    }
}