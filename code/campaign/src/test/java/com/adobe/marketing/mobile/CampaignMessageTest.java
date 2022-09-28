/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;

public class CampaignMessageTest extends BaseTest {
	private CampaignExtension testExtension;

	private static String emptyJsonString = "{}";

	private static String happyFullScreenTemplateJsonString = "{\n" +
			"\t\"messageId\": \"07a1c997-2450-46f0-a454-537906404124\",\n" +
			"\t\"template\": \"fullscreen\",\n" +
			"\t\"payload\": {\n" +
			"\t\t\"html\": \"<!DOCTYPE html><html><head><meta charset=\\\"utf-8\\\" /><title></title><style>.ams-message{display:block;font-size:14px;font-family:\\\"Helvetica Neue\\\",Helvetica,Arial,sans-serif;-webkit-touch-callout:none;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.ams-message a{text-decoration:none}.ams-message-button{position:relative;display:inline-block;white-space:nowrap;overflow:hidden;-o-text-overflow:ellipsis;text-overflow:ellipsis;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;min-width:20px;line-height:44px;height:44px;padding:0 13px;color:#fff;text-align:center;outline:none;margin:0;background-color:#999;border-style:solid;border-width:0 1px 0 1px;border-color:#999;cursor:pointer;}.ams-message-button::after{content:'';display:block;position:absolute;width:100%;height:1px;background-color:rgba(255,255,255,0.3);top:1px;left:0}.ams-message-button.ams-button-confirm{background-color:#007ccf;border-color:#007ccf;}.ams-message-button.ams-button-confirm:hover,.ams-message-button.ams-button-confirm:active{background-color:#0268a0;border-color:#0268a0}.ams-message-button:hover,.ams-message-button:active{background-color:#666;border-color:#666;}.ams-message-button:hover::after,.ams-message-button:active::after{display:none}.ams-message-button:focus{outline:none}.ams-fullscreen-modal{width:100%;height:100%}.ams-fullscreen-wrapper{position:absolute;top:0;width:100%;height:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:215px 0 130px;overflow:hidden;}.ams-fullscreen-wrapper::before{content:'';display:inline-block;height:100%;vertical-align:middle}.ams-fullscreen-image{position:absolute;top:0;left:0;width:100%;height:195px;-webkit-background-size:cover;-moz-background-size:cover;background-size:cover;background-repeat:no-repeat;background-position:center}.ams-fullscreen-text{position:relative;display:inline-block;vertical-align:middle;width:100%;text-align:center;padding:0 44px;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;word-wrap:break-word}.ams-fullscreen-title{display:block;font-size:22px;padding-bottom:20px}.ams-fullscreen.is-theme-dark .ams-fullscreen-title{color:#fff}.ams-fullscreen.is-theme-dark .ams-fullscreen-content{color:#c4c4c4}.ams-fullscreen.is-theme-light .ams-fullscreen-title{color:#3a3a3a}.ams-fullscreen.is-theme-light .ams-fullscreen-content{color:#757575}.ams-fullscreen-buttons{position:absolute;bottom:0;left:0;width:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:0 44px 30px}.ams-fullscreen .ams-message-button{position:relative;display:block;width:100%;height:32px;line-height:32px}.ams-fullscreen-confirm{margin-bottom:16px}.ams-fullscreen{width:100%;height:100%;position:absolute;top:0;left:0;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;}.ams-fullscreen.is-modal-true::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%;background-color:rgba(18,17,18,0.4)}.ams-fullscreen.is-modal-true .ams-fullscreen-modal{position:absolute;top:0;left:0;width:auto;height:auto;top:5%;left:10%;right:10%;bottom:5%}.ams-fullscreen.is-modal-true.is-landscape .ams-fullscreen-modal{top:10%;bottom:10%}.ams-fullscreen.has-no-confirm .ams-fullscreen-confirm{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-cancel{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-confirm{margin:0 !important}.ams-fullscreen.has-no-image .ams-fullscreen-image{display:none}.ams-fullscreen.has-no-image .ams-fullscreen-wrapper{padding:30px 0 135px}.ams-fullscreen.has-no-image .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-theme-dark .ams-fullscreen-modal{background-color:#282828}.ams-fullscreen.is-theme-light .ams-fullscreen-modal{background-color:#ededed}.ams-fullscreen.is-landscape .ams-fullscreen-image{height:100%;width:75%;border:none}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper{right:0;width:304px;padding:25px 0 135px;}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%}.ams-fullscreen.is-landscape .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper{background:rgba(40,40,40,0.7);}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-moz-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-o-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-ms-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:linear-gradient(to right, rgba(40,40,40,0), #282828 30%, #282828)}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper{background:rgba(237,237,237,0.7);}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-moz-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-o-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-ms-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:linear-gradient(to right, rgba(237,237,237,0), #ededed 30%, #ededed)}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-wrapper{padding:30px 0 118px;right:auto;width:100%}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-title{padding-bottom:30px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-buttons{padding-bottom:44px}.ams-fullscreen.is-landscape.has-no-image .ams-message-button{width:38%;width:-webkit-calc(50% - 22px);width:calc(50% - 22px);height:44px;line-height:44px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-confirm{float:left;margin-bottom:0}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-cancel{float:right}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-text,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-text{padding:0 60px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-title,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{padding-bottom:40px;font-size:48px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-content,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-content{font-size:18px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-buttons,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 0 60px;text-align:center}.ams-fullscreen.is-portrait.is-tablet .ams-message-button,.ams-fullscreen.is-landscape.is-tablet .ams-message-button{font-size:18px;height:44px;line-height:44px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-image{height:529px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-wrapper{padding:569px 0 144px}.ams-fullscreen.is-portrait.is-tablet .ams-message-button{width:238px;display:inline-block}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-confirm{margin:0 60px 0 0}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-wrapper{width:40%;padding:40px 0 212px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{font-size:32px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-confirm{margin-bottom:24px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 60px 60px}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-buttons{padding:0 0 60px}.ams-fullscreen.is-portrait.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-message-button{width:320px;display:inline-block;float:none}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-confirm{margin:0 60px 0 0}</style></head><body><div class=\\\"ams-message ams-fullscreen-theme ams-fullscreen is-theme-dark is-modal-true has-no-image has-no-offlineImageUrl has-no-destinationLink has-no-url is-portrait is-phone\\\"><div class=\\\"ams-fullscreen-modal\\\"><div class=\\\"ams-fullscreen-image\\\" style=\\\"background-image: url(&#34;&#34;);\\\"></div><div class=\\\"ams-fullscreen-wrapper\\\"><div class=\\\"ams-fullscreen-text\\\"><div class=\\\"ams-fullscreen-title\\\">Modal Message</div><div class=\\\"ams-fullscreen-content\\\">Content goes here</div></div><div class=\\\"ams-fullscreen-buttons\\\"><a href=\\\"adbinapp://confirm/?url=\\\" class=\\\"ams-fullscreen-confirm ams-message-button ams-button-confirm\\\">Yes</a><a href=\\\"adbinapp://cancel\\\" class=\\\"ams-fullscreen-cancel ams-message-button\\\">No</a></div></div></div></div><script>(window.onresize=function(){var d=document,h=d.documentElement,m=d.body.firstChild,s=' ',c=s+m.className+s,w=h.clientWidth,f=' is-phone ',t=' is-tablet ',l=' is-landscape ',p=' is-portrait ';m.className=c.replace(f,s).replace(t,s).replace(l,s).replace(p,s)+(w<768?f:t)+(w>h.clientHeight?l:p).replace(/^\\\\s+|\\\\s+$/g,'')})()</script></body></html>\\n\",\n"
			+
			"\t\t\"assets\": []\n" +
			"\t},\n" +
			"\t\"showOffline\": false,\n" +
			"\t\"showRule\": \"always\",\n" +
			"\t\"endDate\": 2524730400,\n" +
			"\t\"startDate\": 0,\n" +
			"\t\"audiences\": [],\n" +
			"\t\"triggers\": [{\n" +
			"\t\t\"key\": \"pageName\",\n" +
			"\t\t\"matches\": \"eq\",\n" +
			"\t\t\"values\": [\n" +
			"\t\t\t\"modalMessageTrigger\"\n" +
			"\t\t]\n" +
			"\t}]\n" +
			"}";

	private static String missingMessageIDJsonString = "{\n" +
			"\t\"template\": \"fullscreen\",\n" +
			"\t\"payload\": {\n" +
			"\t\t\"html\": \"<!DOCTYPE html><html><head><meta charset=\\\"utf-8\\\" /><title></title><style>.ams-message{display:block;font-size:14px;font-family:\\\"Helvetica Neue\\\",Helvetica,Arial,sans-serif;-webkit-touch-callout:none;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.ams-message a{text-decoration:none}.ams-message-button{position:relative;display:inline-block;white-space:nowrap;overflow:hidden;-o-text-overflow:ellipsis;text-overflow:ellipsis;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;min-width:20px;line-height:44px;height:44px;padding:0 13px;color:#fff;text-align:center;outline:none;margin:0;background-color:#999;border-style:solid;border-width:0 1px 0 1px;border-color:#999;cursor:pointer;}.ams-message-button::after{content:'';display:block;position:absolute;width:100%;height:1px;background-color:rgba(255,255,255,0.3);top:1px;left:0}.ams-message-button.ams-button-confirm{background-color:#007ccf;border-color:#007ccf;}.ams-message-button.ams-button-confirm:hover,.ams-message-button.ams-button-confirm:active{background-color:#0268a0;border-color:#0268a0}.ams-message-button:hover,.ams-message-button:active{background-color:#666;border-color:#666;}.ams-message-button:hover::after,.ams-message-button:active::after{display:none}.ams-message-button:focus{outline:none}.ams-fullscreen-modal{width:100%;height:100%}.ams-fullscreen-wrapper{position:absolute;top:0;width:100%;height:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:215px 0 130px;overflow:hidden;}.ams-fullscreen-wrapper::before{content:'';display:inline-block;height:100%;vertical-align:middle}.ams-fullscreen-image{position:absolute;top:0;left:0;width:100%;height:195px;-webkit-background-size:cover;-moz-background-size:cover;background-size:cover;background-repeat:no-repeat;background-position:center}.ams-fullscreen-text{position:relative;display:inline-block;vertical-align:middle;width:100%;text-align:center;padding:0 44px;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;word-wrap:break-word}.ams-fullscreen-title{display:block;font-size:22px;padding-bottom:20px}.ams-fullscreen.is-theme-dark .ams-fullscreen-title{color:#fff}.ams-fullscreen.is-theme-dark .ams-fullscreen-content{color:#c4c4c4}.ams-fullscreen.is-theme-light .ams-fullscreen-title{color:#3a3a3a}.ams-fullscreen.is-theme-light .ams-fullscreen-content{color:#757575}.ams-fullscreen-buttons{position:absolute;bottom:0;left:0;width:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:0 44px 30px}.ams-fullscreen .ams-message-button{position:relative;display:block;width:100%;height:32px;line-height:32px}.ams-fullscreen-confirm{margin-bottom:16px}.ams-fullscreen{width:100%;height:100%;position:absolute;top:0;left:0;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;}.ams-fullscreen.is-modal-true::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%;background-color:rgba(18,17,18,0.4)}.ams-fullscreen.is-modal-true .ams-fullscreen-modal{position:absolute;top:0;left:0;width:auto;height:auto;top:5%;left:10%;right:10%;bottom:5%}.ams-fullscreen.is-modal-true.is-landscape .ams-fullscreen-modal{top:10%;bottom:10%}.ams-fullscreen.has-no-confirm .ams-fullscreen-confirm{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-cancel{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-confirm{margin:0 !important}.ams-fullscreen.has-no-image .ams-fullscreen-image{display:none}.ams-fullscreen.has-no-image .ams-fullscreen-wrapper{padding:30px 0 135px}.ams-fullscreen.has-no-image .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-theme-dark .ams-fullscreen-modal{background-color:#282828}.ams-fullscreen.is-theme-light .ams-fullscreen-modal{background-color:#ededed}.ams-fullscreen.is-landscape .ams-fullscreen-image{height:100%;width:75%;border:none}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper{right:0;width:304px;padding:25px 0 135px;}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%}.ams-fullscreen.is-landscape .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper{background:rgba(40,40,40,0.7);}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-moz-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-o-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-ms-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:linear-gradient(to right, rgba(40,40,40,0), #282828 30%, #282828)}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper{background:rgba(237,237,237,0.7);}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-moz-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-o-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-ms-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:linear-gradient(to right, rgba(237,237,237,0), #ededed 30%, #ededed)}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-wrapper{padding:30px 0 118px;right:auto;width:100%}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-title{padding-bottom:30px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-buttons{padding-bottom:44px}.ams-fullscreen.is-landscape.has-no-image .ams-message-button{width:38%;width:-webkit-calc(50% - 22px);width:calc(50% - 22px);height:44px;line-height:44px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-confirm{float:left;margin-bottom:0}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-cancel{float:right}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-text,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-text{padding:0 60px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-title,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{padding-bottom:40px;font-size:48px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-content,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-content{font-size:18px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-buttons,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 0 60px;text-align:center}.ams-fullscreen.is-portrait.is-tablet .ams-message-button,.ams-fullscreen.is-landscape.is-tablet .ams-message-button{font-size:18px;height:44px;line-height:44px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-image{height:529px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-wrapper{padding:569px 0 144px}.ams-fullscreen.is-portrait.is-tablet .ams-message-button{width:238px;display:inline-block}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-confirm{margin:0 60px 0 0}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-wrapper{width:40%;padding:40px 0 212px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{font-size:32px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-confirm{margin-bottom:24px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 60px 60px}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-buttons{padding:0 0 60px}.ams-fullscreen.is-portrait.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-message-button{width:320px;display:inline-block;float:none}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-confirm{margin:0 60px 0 0}</style></head><body><div class=\\\"ams-message ams-fullscreen-theme ams-fullscreen is-theme-dark is-modal-true has-no-image has-no-offlineImageUrl has-no-destinationLink has-no-url is-portrait is-phone\\\"><div class=\\\"ams-fullscreen-modal\\\"><div class=\\\"ams-fullscreen-image\\\" style=\\\"background-image: url(&#34;&#34;);\\\"></div><div class=\\\"ams-fullscreen-wrapper\\\"><div class=\\\"ams-fullscreen-text\\\"><div class=\\\"ams-fullscreen-title\\\">Modal Message</div><div class=\\\"ams-fullscreen-content\\\">Content goes here</div></div><div class=\\\"ams-fullscreen-buttons\\\"><a href=\\\"adbinapp://confirm/?url=\\\" class=\\\"ams-fullscreen-confirm ams-message-button ams-button-confirm\\\">Yes</a><a href=\\\"adbinapp://cancel\\\" class=\\\"ams-fullscreen-cancel ams-message-button\\\">No</a></div></div></div></div><script>(window.onresize=function(){var d=document,h=d.documentElement,m=d.body.firstChild,s=' ',c=s+m.className+s,w=h.clientWidth,f=' is-phone ',t=' is-tablet ',l=' is-landscape ',p=' is-portrait ';m.className=c.replace(f,s).replace(t,s).replace(l,s).replace(p,s)+(w<768?f:t)+(w>h.clientHeight?l:p).replace(/^\\\\s+|\\\\s+$/g,'')})()</script></body></html>\\n\",\n"
			+
			"\t\t\"assets\": []\n" +
			"\t},\n" +
			"\t\"showOffline\": false,\n" +
			"\t\"showRule\": \"always\",\n" +
			"\t\"endDate\": 2524730400,\n" +
			"\t\"startDate\": 0,\n" +
			"\t\"audiences\": [],\n" +
			"\t\"triggers\": [{\n" +
			"\t\t\"key\": \"pageName\",\n" +
			"\t\t\"matches\": \"eq\",\n" +
			"\t\t\"values\": [\n" +
			"\t\t\t\"modalMessageTrigger\"\n" +
			"\t\t]\n" +
			"\t}]\n" +
			"}";

	private static String emptyMessageIDJsonString = "{\n" +
			"\t\"messageId\": \"\",\n" +
			"\t\"template\": \"fullscreen\",\n" +
			"\t\"payload\": {\n" +
			"\t\t\"html\": \"<!DOCTYPE html><html><head><meta charset=\\\"utf-8\\\" /><title></title><style>.ams-message{display:block;font-size:14px;font-family:\\\"Helvetica Neue\\\",Helvetica,Arial,sans-serif;-webkit-touch-callout:none;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.ams-message a{text-decoration:none}.ams-message-button{position:relative;display:inline-block;white-space:nowrap;overflow:hidden;-o-text-overflow:ellipsis;text-overflow:ellipsis;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;min-width:20px;line-height:44px;height:44px;padding:0 13px;color:#fff;text-align:center;outline:none;margin:0;background-color:#999;border-style:solid;border-width:0 1px 0 1px;border-color:#999;cursor:pointer;}.ams-message-button::after{content:'';display:block;position:absolute;width:100%;height:1px;background-color:rgba(255,255,255,0.3);top:1px;left:0}.ams-message-button.ams-button-confirm{background-color:#007ccf;border-color:#007ccf;}.ams-message-button.ams-button-confirm:hover,.ams-message-button.ams-button-confirm:active{background-color:#0268a0;border-color:#0268a0}.ams-message-button:hover,.ams-message-button:active{background-color:#666;border-color:#666;}.ams-message-button:hover::after,.ams-message-button:active::after{display:none}.ams-message-button:focus{outline:none}.ams-fullscreen-modal{width:100%;height:100%}.ams-fullscreen-wrapper{position:absolute;top:0;width:100%;height:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:215px 0 130px;overflow:hidden;}.ams-fullscreen-wrapper::before{content:'';display:inline-block;height:100%;vertical-align:middle}.ams-fullscreen-image{position:absolute;top:0;left:0;width:100%;height:195px;-webkit-background-size:cover;-moz-background-size:cover;background-size:cover;background-repeat:no-repeat;background-position:center}.ams-fullscreen-text{position:relative;display:inline-block;vertical-align:middle;width:100%;text-align:center;padding:0 44px;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;word-wrap:break-word}.ams-fullscreen-title{display:block;font-size:22px;padding-bottom:20px}.ams-fullscreen.is-theme-dark .ams-fullscreen-title{color:#fff}.ams-fullscreen.is-theme-dark .ams-fullscreen-content{color:#c4c4c4}.ams-fullscreen.is-theme-light .ams-fullscreen-title{color:#3a3a3a}.ams-fullscreen.is-theme-light .ams-fullscreen-content{color:#757575}.ams-fullscreen-buttons{position:absolute;bottom:0;left:0;width:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:0 44px 30px}.ams-fullscreen .ams-message-button{position:relative;display:block;width:100%;height:32px;line-height:32px}.ams-fullscreen-confirm{margin-bottom:16px}.ams-fullscreen{width:100%;height:100%;position:absolute;top:0;left:0;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;}.ams-fullscreen.is-modal-true::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%;background-color:rgba(18,17,18,0.4)}.ams-fullscreen.is-modal-true .ams-fullscreen-modal{position:absolute;top:0;left:0;width:auto;height:auto;top:5%;left:10%;right:10%;bottom:5%}.ams-fullscreen.is-modal-true.is-landscape .ams-fullscreen-modal{top:10%;bottom:10%}.ams-fullscreen.has-no-confirm .ams-fullscreen-confirm{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-cancel{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-confirm{margin:0 !important}.ams-fullscreen.has-no-image .ams-fullscreen-image{display:none}.ams-fullscreen.has-no-image .ams-fullscreen-wrapper{padding:30px 0 135px}.ams-fullscreen.has-no-image .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-theme-dark .ams-fullscreen-modal{background-color:#282828}.ams-fullscreen.is-theme-light .ams-fullscreen-modal{background-color:#ededed}.ams-fullscreen.is-landscape .ams-fullscreen-image{height:100%;width:75%;border:none}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper{right:0;width:304px;padding:25px 0 135px;}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%}.ams-fullscreen.is-landscape .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper{background:rgba(40,40,40,0.7);}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-moz-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-o-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-ms-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:linear-gradient(to right, rgba(40,40,40,0), #282828 30%, #282828)}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper{background:rgba(237,237,237,0.7);}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-moz-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-o-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-ms-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:linear-gradient(to right, rgba(237,237,237,0), #ededed 30%, #ededed)}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-wrapper{padding:30px 0 118px;right:auto;width:100%}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-title{padding-bottom:30px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-buttons{padding-bottom:44px}.ams-fullscreen.is-landscape.has-no-image .ams-message-button{width:38%;width:-webkit-calc(50% - 22px);width:calc(50% - 22px);height:44px;line-height:44px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-confirm{float:left;margin-bottom:0}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-cancel{float:right}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-text,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-text{padding:0 60px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-title,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{padding-bottom:40px;font-size:48px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-content,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-content{font-size:18px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-buttons,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 0 60px;text-align:center}.ams-fullscreen.is-portrait.is-tablet .ams-message-button,.ams-fullscreen.is-landscape.is-tablet .ams-message-button{font-size:18px;height:44px;line-height:44px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-image{height:529px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-wrapper{padding:569px 0 144px}.ams-fullscreen.is-portrait.is-tablet .ams-message-button{width:238px;display:inline-block}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-confirm{margin:0 60px 0 0}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-wrapper{width:40%;padding:40px 0 212px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{font-size:32px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-confirm{margin-bottom:24px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 60px 60px}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-buttons{padding:0 0 60px}.ams-fullscreen.is-portrait.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-message-button{width:320px;display:inline-block;float:none}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-confirm{margin:0 60px 0 0}</style></head><body><div class=\\\"ams-message ams-fullscreen-theme ams-fullscreen is-theme-dark is-modal-true has-no-image has-no-offlineImageUrl has-no-destinationLink has-no-url is-portrait is-phone\\\"><div class=\\\"ams-fullscreen-modal\\\"><div class=\\\"ams-fullscreen-image\\\" style=\\\"background-image: url(&#34;&#34;);\\\"></div><div class=\\\"ams-fullscreen-wrapper\\\"><div class=\\\"ams-fullscreen-text\\\"><div class=\\\"ams-fullscreen-title\\\">Modal Message</div><div class=\\\"ams-fullscreen-content\\\">Content goes here</div></div><div class=\\\"ams-fullscreen-buttons\\\"><a href=\\\"adbinapp://confirm/?url=\\\" class=\\\"ams-fullscreen-confirm ams-message-button ams-button-confirm\\\">Yes</a><a href=\\\"adbinapp://cancel\\\" class=\\\"ams-fullscreen-cancel ams-message-button\\\">No</a></div></div></div></div><script>(window.onresize=function(){var d=document,h=d.documentElement,m=d.body.firstChild,s=' ',c=s+m.className+s,w=h.clientWidth,f=' is-phone ',t=' is-tablet ',l=' is-landscape ',p=' is-portrait ';m.className=c.replace(f,s).replace(t,s).replace(l,s).replace(p,s)+(w<768?f:t)+(w>h.clientHeight?l:p).replace(/^\\\\s+|\\\\s+$/g,'')})()</script></body></html>\\n\",\n"
			+
			"\t\t\"assets\": []\n" +
			"\t},\n" +
			"\t\"showOffline\": false,\n" +
			"\t\"showRule\": \"always\",\n" +
			"\t\"endDate\": 2524730400,\n" +
			"\t\"startDate\": 0,\n" +
			"\t\"audiences\": [],\n" +
			"\t\"triggers\": [{\n" +
			"\t\t\"key\": \"pageName\",\n" +
			"\t\t\"matches\": \"eq\",\n" +
			"\t\t\"values\": [\n" +
			"\t\t\t\"modalMessageTrigger\"\n" +
			"\t\t]\n" +
			"\t}]\n" +
			"}";

	private static String unsupportedTemplateJsonString = "{\n" +
			"\t\"messageId\": \"07a1c997-2450-46f0-a454-537906404124\",\n" +
			"\t\"template\": \"random-template\",\n" +
			"\t\"payload\": {\n" +
			"\t\t\"html\": \"<!DOCTYPE html><html><head><meta charset=\\\"utf-8\\\" /><title></title><style>.ams-message{display:block;font-size:14px;font-family:\\\"Helvetica Neue\\\",Helvetica,Arial,sans-serif;-webkit-touch-callout:none;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.ams-message a{text-decoration:none}.ams-message-button{position:relative;display:inline-block;white-space:nowrap;overflow:hidden;-o-text-overflow:ellipsis;text-overflow:ellipsis;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;min-width:20px;line-height:44px;height:44px;padding:0 13px;color:#fff;text-align:center;outline:none;margin:0;background-color:#999;border-style:solid;border-width:0 1px 0 1px;border-color:#999;cursor:pointer;}.ams-message-button::after{content:'';display:block;position:absolute;width:100%;height:1px;background-color:rgba(255,255,255,0.3);top:1px;left:0}.ams-message-button.ams-button-confirm{background-color:#007ccf;border-color:#007ccf;}.ams-message-button.ams-button-confirm:hover,.ams-message-button.ams-button-confirm:active{background-color:#0268a0;border-color:#0268a0}.ams-message-button:hover,.ams-message-button:active{background-color:#666;border-color:#666;}.ams-message-button:hover::after,.ams-message-button:active::after{display:none}.ams-message-button:focus{outline:none}.ams-fullscreen-modal{width:100%;height:100%}.ams-fullscreen-wrapper{position:absolute;top:0;width:100%;height:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:215px 0 130px;overflow:hidden;}.ams-fullscreen-wrapper::before{content:'';display:inline-block;height:100%;vertical-align:middle}.ams-fullscreen-image{position:absolute;top:0;left:0;width:100%;height:195px;-webkit-background-size:cover;-moz-background-size:cover;background-size:cover;background-repeat:no-repeat;background-position:center}.ams-fullscreen-text{position:relative;display:inline-block;vertical-align:middle;width:100%;text-align:center;padding:0 44px;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;word-wrap:break-word}.ams-fullscreen-title{display:block;font-size:22px;padding-bottom:20px}.ams-fullscreen.is-theme-dark .ams-fullscreen-title{color:#fff}.ams-fullscreen.is-theme-dark .ams-fullscreen-content{color:#c4c4c4}.ams-fullscreen.is-theme-light .ams-fullscreen-title{color:#3a3a3a}.ams-fullscreen.is-theme-light .ams-fullscreen-content{color:#757575}.ams-fullscreen-buttons{position:absolute;bottom:0;left:0;width:100%;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;padding:0 44px 30px}.ams-fullscreen .ams-message-button{position:relative;display:block;width:100%;height:32px;line-height:32px}.ams-fullscreen-confirm{margin-bottom:16px}.ams-fullscreen{width:100%;height:100%;position:absolute;top:0;left:0;-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box;}.ams-fullscreen.is-modal-true::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%;background-color:rgba(18,17,18,0.4)}.ams-fullscreen.is-modal-true .ams-fullscreen-modal{position:absolute;top:0;left:0;width:auto;height:auto;top:5%;left:10%;right:10%;bottom:5%}.ams-fullscreen.is-modal-true.is-landscape .ams-fullscreen-modal{top:10%;bottom:10%}.ams-fullscreen.has-no-confirm .ams-fullscreen-confirm{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-cancel{display:none !important}.ams-fullscreen.has-no-cancel .ams-fullscreen-confirm{margin:0 !important}.ams-fullscreen.has-no-image .ams-fullscreen-image{display:none}.ams-fullscreen.has-no-image .ams-fullscreen-wrapper{padding:30px 0 135px}.ams-fullscreen.has-no-image .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-theme-dark .ams-fullscreen-modal{background-color:#282828}.ams-fullscreen.is-theme-light .ams-fullscreen-modal{background-color:#ededed}.ams-fullscreen.is-landscape .ams-fullscreen-image{height:100%;width:75%;border:none}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper{right:0;width:304px;padding:25px 0 135px;}.ams-fullscreen.is-landscape .ams-fullscreen-wrapper::before{content:'';position:absolute;top:0;left:0;width:100%;height:100%}.ams-fullscreen.is-landscape .ams-fullscreen-title{padding-bottom:25px}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper{background:rgba(40,40,40,0.7);}.ams-fullscreen.is-landscape.is-theme-dark .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-moz-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-o-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:-ms-linear-gradient(left, rgba(40,40,40,0), #282828 30%, #282828);background:linear-gradient(to right, rgba(40,40,40,0), #282828 30%, #282828)}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper{background:rgba(237,237,237,0.7);}.ams-fullscreen.is-landscape.is-theme-light .ams-fullscreen-wrapper::before{background:-webkit-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-moz-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-o-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:-ms-linear-gradient(left, rgba(237,237,237,0), #ededed 30%, #ededed);background:linear-gradient(to right, rgba(237,237,237,0), #ededed 30%, #ededed)}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-wrapper{padding:30px 0 118px;right:auto;width:100%}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-title{padding-bottom:30px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-buttons{padding-bottom:44px}.ams-fullscreen.is-landscape.has-no-image .ams-message-button{width:38%;width:-webkit-calc(50% - 22px);width:calc(50% - 22px);height:44px;line-height:44px}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-confirm{float:left;margin-bottom:0}.ams-fullscreen.is-landscape.has-no-image .ams-fullscreen-cancel{float:right}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-text,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-text{padding:0 60px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-title,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{padding-bottom:40px;font-size:48px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-content,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-content{font-size:18px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-buttons,.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 0 60px;text-align:center}.ams-fullscreen.is-portrait.is-tablet .ams-message-button,.ams-fullscreen.is-landscape.is-tablet .ams-message-button{font-size:18px;height:44px;line-height:44px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-image{height:529px}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-wrapper{padding:569px 0 144px}.ams-fullscreen.is-portrait.is-tablet .ams-message-button{width:238px;display:inline-block}.ams-fullscreen.is-portrait.is-tablet .ams-fullscreen-confirm{margin:0 60px 0 0}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-wrapper{width:40%;padding:40px 0 212px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-title{font-size:32px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-confirm{margin-bottom:24px}.ams-fullscreen.is-landscape.is-tablet .ams-fullscreen-buttons{padding:0 60px 60px}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-buttons{padding:0 0 60px}.ams-fullscreen.is-portrait.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-wrapper{padding:40px 0 144px;width:100%}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-message-button{width:320px;display:inline-block;float:none}.ams-fullscreen.is-landscape.is-tablet.has-no-image .ams-fullscreen-confirm{margin:0 60px 0 0}</style></head><body><div class=\\\"ams-message ams-fullscreen-theme ams-fullscreen is-theme-dark is-modal-true has-no-image has-no-offlineImageUrl has-no-destinationLink has-no-url is-portrait is-phone\\\"><div class=\\\"ams-fullscreen-modal\\\"><div class=\\\"ams-fullscreen-image\\\" style=\\\"background-image: url(&#34;&#34;);\\\"></div><div class=\\\"ams-fullscreen-wrapper\\\"><div class=\\\"ams-fullscreen-text\\\"><div class=\\\"ams-fullscreen-title\\\">Modal Message</div><div class=\\\"ams-fullscreen-content\\\">Content goes here</div></div><div class=\\\"ams-fullscreen-buttons\\\"><a href=\\\"adbinapp://confirm/?url=\\\" class=\\\"ams-fullscreen-confirm ams-message-button ams-button-confirm\\\">Yes</a><a href=\\\"adbinapp://cancel\\\" class=\\\"ams-fullscreen-cancel ams-message-button\\\">No</a></div></div></div></div><script>(window.onresize=function(){var d=document,h=d.documentElement,m=d.body.firstChild,s=' ',c=s+m.className+s,w=h.clientWidth,f=' is-phone ',t=' is-tablet ',l=' is-landscape ',p=' is-portrait ';m.className=c.replace(f,s).replace(t,s).replace(l,s).replace(p,s)+(w<768?f:t)+(w>h.clientHeight?l:p).replace(/^\\\\s+|\\\\s+$/g,'')})()</script></body></html>\\n\",\n"
			+
			"\t\t\"assets\": []\n" +
			"\t},\n" +
			"\t\"showOffline\": false,\n" +
			"\t\"showRule\": \"always\",\n" +
			"\t\"endDate\": 2524730400,\n" +
			"\t\"startDate\": 0,\n" +
			"\t\"audiences\": [],\n" +
			"\t\"triggers\": [{\n" +
			"\t\t\"key\": \"pageName\",\n" +
			"\t\t\"matches\": \"eq\",\n" +
			"\t\t\"values\": [\n" +
			"\t\t\t\"modalMessageTrigger\"\n" +
			"\t\t]\n" +
			"\t}]\n" +
			"}";

	private static String missingTemplateJsonString = "{\n" +
			"      \"messageId\": \"b8902e6c-4eb9-497c-9ebd-20d3a656d7fb\",\n" +
			"      \"payload\": {\n" +
			"        \"templateurl\": \"http://docs.google.com/forms/d/e/1FAIpQLSfac0DGZcWzWp-sIHGm4hN2MlkwQc6NFxSxUr47kGj6vOKAJw/formResponse?entry.2145377890={testName}&entry.859563890={testMessage}&submit=Submit\",\n"
			+
			"        \"templatebody\": \"\",\n" +
			"        \"contenttype\": \"\",\n" +
			"        \"timeout\": 2\n" +
			"      },\n" +
			"      \"showOffline\": true,\n" +
			"      \"showRule\": \"always\",\n" +
			"      \"endDate\": 2524730400,\n" +
			"      \"startDate\": 0,\n" +
			"      \"audiences\": [],\n" +
			"      \"triggers\": [\n" +
			"        {\n" +
			"          \"key\": \"pev2\",\n" +
			"          \"matches\": \"eq\",\n" +
			"          \"values\": [\n" +
			"            \"AMACTION:demo-postbacks\"\n" +
			"          ]\n" +
			"        }\n" +
			"      ]\n" +
			"    }";

	private HashMap<String, Variant> happyMessageMap;
	private HashMap<String, Variant> happyDetailMap;
	private ArrayList<ArrayList<String>> happyRemoteAssets;
	private ArrayList<String> remoteAssetOne;
	private ArrayList<String> remoteAssetTwo;

	private String assetsPath;
	private MockCampaignMessage mockCampaignMessage;

	@Before()
	public void setup() {
		super.beforeEach();
		remoteAssetOne = new ArrayList<String>();
		remoteAssetOne.add("http://asset1-url00.jpeg");
		remoteAssetOne.add("http://asset1-url01.jpeg");
		remoteAssetOne.add("01.jpeg");

		remoteAssetTwo = new ArrayList<String>();
		remoteAssetTwo.add("http://asset2-url10.jpeg");
		remoteAssetTwo.add("http://asset2-url11.jpeg");

		happyRemoteAssets = new ArrayList<ArrayList<String>>();
		happyRemoteAssets.add(remoteAssetOne);
		happyRemoteAssets.add(remoteAssetTwo);

		happyDetailMap = new HashMap<String, Variant>();
		happyDetailMap.put("template", Variant.fromString("fullscreen"));
		happyDetailMap.put("html", Variant.fromString("<html>everything is awesome http://asset1-url00.jpeg</html>"));
		happyDetailMap.put("remoteAssets", Variant.fromTypedList(happyRemoteAssets,
						   new TypedListVariantSerializer<String>(new StringVariantSerializer())));

		File assetFile = getResource("happy_test.html");

		if (assetFile != null) {
			assetsPath = assetFile.getParent();
		}

		happyMessageMap = new HashMap<String, Variant>();
		happyMessageMap.put("id", Variant.fromString("07a1c997-2450-46f0-a454-537906404124"));
		happyMessageMap.put("type", Variant.fromString("iam"));
		happyMessageMap.put("assetsPath", Variant.fromString(assetsPath));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		testExtension = new CampaignExtension(eventHub, platformServices);
	}

	CampaignRuleConsequence createCampaignRuleConsequence(Map<String, Variant> consequenceMap) {
		Variant consequenceAsVariant = Variant.fromVariantMap(consequenceMap);
		CampaignRuleConsequence consequence;

		try {
			consequence = consequenceAsVariant.getTypedObject(new CampaignRuleConsequenceSerializer());
		} catch (VariantException ex) {
			consequence = null;
		}

		return consequence;
	}

	@Test
	public void createMessageShouldInstantiateFullScreen_When_FullScreenTemplate() throws Exception {
		CampaignMessage msg = CampaignMessage.createMessageObject(testExtension, platformServices,
							  createCampaignRuleConsequence(happyMessageMap));

		assertNotNull(msg);
		assertTrue(msg instanceof FullScreenMessage);
	}

	@Test(expected = MissingPlatformServicesException.class)
	public void createMessageShouldThrowMissingPlatformServicesException_When_NullPlatformServices() throws Exception {
		CampaignMessage.createMessageObject(testExtension, null, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void createMessageShouldReturnNullMessage_When_NullConsequence() throws Exception {
		CampaignMessage.createMessageObject(testExtension, platformServices, null);
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void createMessageShouldThrowMissingMessageFieldException_When_MissingTemplate() throws Exception {
		// setup
		happyDetailMap.remove("template");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		CampaignMessage.createMessageObject(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test
	public void createMessageShouldReturnNullMessage_When_UnsupportedTemplate() {
		// setup
		happyDetailMap.put("template", Variant.fromString("notSupported"));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		CampaignMessage msg = null;

		try {
			msg = CampaignMessage.createMessageObject(testExtension, platformServices,
					createCampaignRuleConsequence(happyMessageMap));
		} catch (Exception ex) {
			assertNotNull(ex);
			ex.printStackTrace();
		}

		// verify
		assertNull(msg);
	}

	@Test(expected = MissingPlatformServicesException.class)
	public void MessageConstructor_ThrowsMissingPlatformServicesException_When_NullPlatformServices() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		new CampaignMessage(testExtension, null, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void MessageConstructor_ThrowsCampaignMessageRequiredFieldMissingException_When_NoMessageID() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyMessageMap.remove("id");

		// test
		new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void MessageConstructor_ThrowsCampaignMessageRequiredFieldMissingException_When_EmptyMessageID() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyMessageMap.put("id", Variant.fromString(""));

		// test
		new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void MessageConstructor_ThrowsCampaignMessageRequiredFieldMissingException_When_NoMessageType() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyMessageMap.remove("type");

		// test
		new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void MessageConstructor_ThrowsCampaignMessageRequiredFieldMissingException_When_EmptyMessageType() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyMessageMap.put("type", Variant.fromString(""));

		// test
		new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void MessageConstructor_ThrowsCampaignMessageRequiredFieldMissingException_When_InvalidMessageType() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyMessageMap.put("type", Variant.fromString("invalid"));

		// test
		new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void MessageConstructor_ThrowsCampaignMessageRequiredFieldMissingException_When_NoMessageDetail() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyMessageMap.remove("detail");

		// test
		new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void MessageConstructor_ThrowsCampaignMessageRequiredFieldMissingException_When_EmptyMessageDetail() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyMessageMap.put("detail", Variant.fromVariantMap(new HashMap<String, Variant>()));

		// test
		new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
			@Override
			boolean shouldDownloadAssets() {
				return false;
			}

			@Override
			void showMessage() {}
		};
	}

	@Test
	public void MessageConstructor_createsMessageObject__When_HappyJson() {

		CampaignMessage msg = null;

		try {
			msg = new CampaignMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap)) {
				@Override
				boolean shouldDownloadAssets() {
					return false;
				}

				@Override
				void showMessage() {}
			};
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertNotNull(msg);
	}

	@Test
	public void triggered_ShouldInvokeCallDispatchMessageInteraction_WithCorrectParameters() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		mockCampaignMessage.triggered();

		// verify
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionWasCalled);
		assertNotNull(mockCampaignMessage.callDispatchMessageInteractionParameterData);
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.size(), 2);
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_ID));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_ID),
					 "07a1c997-2450-46f0-a454-537906404124");
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_TRIGGERED));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_TRIGGERED),
					 "1");
	}

	@Test
	public void viewed_ShouldInvokeCallDispatchMessageInteractiona_WithCorrectParameters() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		mockCampaignMessage.viewed();

		// verify
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionWasCalled);
		assertNotNull(mockCampaignMessage.callDispatchMessageInteractionParameterData);
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.size(), 2);
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_ID));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_ID),
					 "07a1c997-2450-46f0-a454-537906404124");
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED),
					 "1");
	}

	@Test
	public void clickedThrough_ShouldInvokeDispatchCallForData_WithCorrectParameters() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		mockCampaignMessage.clickedThrough();

		// verify
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionWasCalled);
		assertNotNull(mockCampaignMessage.callDispatchMessageInteractionParameterData);
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.size(), 2);
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_ID));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_ID),
					 "07a1c997-2450-46f0-a454-537906404124");
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_CLICKED));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_CLICKED),
					 "1");
	}

	@Test
	public void clickedWithData_ShouldInvokeDispatchCallForData_WithCorrectParameters() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		mockCampaignMessage.clickedWithData(new HashMap<String, String>() {
			{
				put("someKey", "someValue");
				put("url", "https://www.adobe.com");
			}
		});

		// verify
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionWasCalled);
		assertNotNull(mockCampaignMessage.callDispatchMessageInteractionParameterData);
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.size(), 4);
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_ID));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_ID),
					 "07a1c997-2450-46f0-a454-537906404124");
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey(
					   CampaignTestConstants.ContextDataKeys.MESSAGE_CLICKED));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get(
						 CampaignTestConstants.ContextDataKeys.MESSAGE_CLICKED),
					 "1");
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey("someKey"));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get("someKey"), "someValue");
		assertTrue(mockCampaignMessage.callDispatchMessageInteractionParameterData.containsKey("url"));
		assertEquals(mockCampaignMessage.callDispatchMessageInteractionParameterData.get("url"), "https://www.adobe.com");
		// openUrl is called
		assertTrue(mockCampaignMessage.openUrlWasCalled);
		assertEquals(mockCampaignMessage.openUrlParameterUrl, "https://www.adobe.com");
	}

	@Test
	public void openUrl_ShouldInvokeUIServiceShowUrl_Happy() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		mockCampaignMessage.openUrl("https://www.adobe.com");

		// verify
		assertTrue(((FakeUIService)platformServices.getUIService()).showUrlWasCalled);
		assertEquals(((FakeUIService)platformServices.getUIService()).showUrlUrl, "https://www.adobe.com");
	}

	@Test
	public void openUrl_ShouldNotInvokeUIServiceShowUrl_When_NullUrl() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		mockCampaignMessage.openUrl(null);

		// verify
		assertFalse(((FakeUIService)platformServices.getUIService()).showUrlWasCalled);
	}

	@Test
	public void openUrl_ShouldNotInvokeUIServiceShowUrl_When_EmptyUrl() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		mockCampaignMessage.openUrl("");

		// verify
		assertFalse(((FakeUIService)platformServices.getUIService()).showUrlWasCalled);
	}

	@Test
	public void expandTokens_ShouldReturnEmptyString_When_EmptyInput() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens("", new HashMap<String, String>() {
			{
				put("key1", "value1");
			}
		});

		// verify
		assertEquals(output, "");
	}

	@Test
	public void expandTokens_ShouldReturnNullString_When_NullInput() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens(null, new HashMap<String, String>() {
			{
				put("key1", "value1");
			}
		});

		// verify
		assertNull(output);
	}

	@Test
	public void expandTokens_ShouldReturnInputString_When_NullTokens() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens("This key1 should not be equal to value1.", null);

		// verify
		assertEquals("This key1 should not be equal to value1.", output);
	}

	@Test
	public void expandTokens_ShouldReturnInputString_When_EmptyTokens() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens("This key1 should not be equal to value1.",
							  new HashMap<String, String>());

		// verify
		assertEquals("This key1 should not be equal to value1.", output);
	}

	@Test
	public void expandTokens_ShouldReturnStringWithoutTokensReplaced_When_ValidInputAndNonMatchingTokens() throws
		Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens("This key1 should not be equal to value1.",
		new HashMap<String, String>() {
			{
				put("key2", "value2");
			}
		});

		// verify
		assertEquals("This key1 should not be equal to value1.", output);
	}

	@Test
	public void expandTokens_ShouldReturnStringWithTokensReplaced_When_ValidInputAndMatchingTokens() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens("This key1 should be equal to value1.",
		new HashMap<String, String>() {
			{
				put("key1", "value1");
			}
		});

		// verify
		assertEquals("This value1 should be equal to value1.", output);
	}

	@Test
	public void expandTokens_ShouldIgnoreNullTokenKeyValue_When_ValidInputAndNullTokenKey() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens("This key1 should not be equal to value1.",
		new HashMap<String, String>() {
			{
				put(null, "value1");
			}
		});

		// verify
		assertEquals("This key1 should not be equal to value1.", output);
	}

	@Test
	public void expandTokens_ShouldIgnoreNullTokenKeyValue_When_ValidInputAndNullTokenValue() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output = mockCampaignMessage.expandTokens("This key1 should not be equal to value1.",
		new HashMap<String, String>() {
			{
				put("key1", "value1");
				put("key1", null);
			}
		});

		// verify
		assertEquals("This key1 should not be equal to value1.", output);
	}

	@Test
	public void expandTokens_ShouldWorkWithMultipleTokens_When_ValidInputAndMixedTokens() throws Exception {
		// setup
		mockCampaignMessage = new MockCampaignMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		final String output =
			mockCampaignMessage.expandTokens("This key1 should be equal to value1. This key2 should be equal to value2.",
		new HashMap<String, String>() {
			{
				put("key1", "value1");
				put(null, "value1");
				put("key2", null);
				put(null, null);
				put("key2", "value2");

			}
		});

		// verify
		assertEquals("This value1 should be equal to value1. This value2 should be equal to value2.", output);
	}

}
