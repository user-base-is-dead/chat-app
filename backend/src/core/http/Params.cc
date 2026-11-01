#include "core/http/Params.h"

#include <charconv>

#include "core/http/ApiError.h"

namespace core::http
{
int64_t parseId(const std::string &value, const char *name)
{
    int64_t id = 0;
    const auto [end, error] = std::from_chars(value.data(), value.data() + value.size(), id);
    if (error != std::errc() || end != value.data() + value.size() || id <= 0)
        throw ApiError(drogon::k400BadRequest, "invalid_id", std::string(name) + " must be a valid id");
    return id;
}

int64_t parseOptionalId(const std::string &value, const char *name)
{
    return value.empty() ? 0 : parseId(value, name);
}
}
