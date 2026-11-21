#pragma once

#include <drogon/HttpController.h>

#include "features/devices/DeviceRepository.h"

namespace features::push
{
class PushController : public drogon::HttpController<PushController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(PushController::save, "/v1/devices/me/push", drogon::Put, "core::auth::AuthFilter");
    ADD_METHOD_TO(PushController::remove, "/v1/devices/me/push", drogon::Delete, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> save(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> remove(drogon::HttpRequestPtr request);

  private:
    devices::DeviceRepository devices_;
};
}
