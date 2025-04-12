# -- encoding: utf-8 --
# Copyright (c) Huawei Technologies Co., Ltd. -. All rights reserved.
import typing

from .serializable import Serializable
from .media import Media


class Document(Serializable):
    """
    Document.
    """
    content: str
    media: Media = None
    metadata: typing.Dict[str, object]

    class Config:
        frozen = True
        smart_union = True
