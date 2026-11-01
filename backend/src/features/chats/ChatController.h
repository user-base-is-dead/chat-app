#pragma once

#include <string>

#include <drogon/HttpController.h>

#include "features/chats/ChatService.h"

namespace features::chats
{
class ChatController : public drogon::HttpController<ChatController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(ChatController::list, "/v1/chats", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::openDm, "/v1/chats/dm", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::accept, "/v1/chats/{1}/accept", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::restrictChat, "/v1/chats/{1}/restrict", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::unrestrictChat, "/v1/chats/{1}/unrestrict", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::pin, "/v1/chats/{1}/pin", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::unpin, "/v1/chats/{1}/unpin", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::mute, "/v1/chats/{1}/mute", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::unmute, "/v1/chats/{1}/unmute", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::remove, "/v1/chats/{1}", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::messages, "/v1/chats/{1}/messages", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::send, "/v1/chats/{1}/messages", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::edit, "/v1/chats/{1}/messages/{2}", drogon::Patch, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::deleteMessage, "/v1/chats/{1}/messages/{2}", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChatController::markRead, "/v1/chats/{1}/read", drogon::Post, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> list(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> openDm(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> accept(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> restrictChat(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> unrestrictChat(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> pin(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> unpin(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> mute(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> unmute(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> remove(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> messages(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> send(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> edit(drogon::HttpRequestPtr request, std::string chatId, std::string messageId);
    drogon::Task<drogon::HttpResponsePtr> deleteMessage(drogon::HttpRequestPtr request,
                                                        std::string chatId,
                                                        std::string messageId);
    drogon::Task<drogon::HttpResponsePtr> markRead(drogon::HttpRequestPtr request, std::string chatId);

  private:
    ChatService service_;
};
}
