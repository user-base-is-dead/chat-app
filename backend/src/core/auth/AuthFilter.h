#pragma once

#include <cstdint>

#include <drogon/HttpFilter.h>

namespace core::auth
{
class AuthFilter : public drogon::HttpFilter<AuthFilter>
{
  public:
    void doFilter(const drogon::HttpRequestPtr &request,
                  drogon::FilterCallback &&reject,
                  drogon::FilterChainCallback &&next) override;
};

int64_t currentUserId(const drogon::HttpRequestPtr &request);
int64_t currentDeviceId(const drogon::HttpRequestPtr &request);
}
