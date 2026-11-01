#pragma once

#include <cstdint>
#include <mutex>
#include <string>
#include <unordered_map>
#include <unordered_set>

#include <drogon/WebSocketConnection.h>
#include <json/value.h>

namespace features::realtime
{
class Hub
{
  public:
    bool add(int64_t userId, const drogon::WebSocketConnectionPtr &connection);
    bool remove(int64_t userId, const drogon::WebSocketConnectionPtr &connection);
    bool isOnline(int64_t userId) const;
    void send(int64_t userId, const std::string &event, const Json::Value &data) const;
    void disconnect(int64_t userId) const;

  private:
    mutable std::mutex mutex_;
    std::unordered_map<int64_t, std::unordered_set<drogon::WebSocketConnectionPtr>> connections_;
};

Hub &hub();
}
