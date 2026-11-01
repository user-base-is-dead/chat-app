#pragma once

#include <drogon/WebSocketController.h>

namespace features::realtime
{
class RealtimeController : public drogon::WebSocketController<RealtimeController>
{
  public:
    WS_PATH_LIST_BEGIN
    WS_PATH_ADD("/v1/ws");
    WS_PATH_LIST_END

    void handleNewConnection(const drogon::HttpRequestPtr &request,
                             const drogon::WebSocketConnectionPtr &connection) override;
    void handleNewMessage(const drogon::WebSocketConnectionPtr &connection,
                          std::string &&message,
                          const drogon::WebSocketMessageType &type) override;
    void handleConnectionClosed(const drogon::WebSocketConnectionPtr &connection) override;
};
}
